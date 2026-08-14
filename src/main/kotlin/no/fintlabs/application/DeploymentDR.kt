package no.fintlabs.application

import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.ContainerPort
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.HTTPGetAction
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.LabelSelector
import io.fabric8.kubernetes.api.model.Probe
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.Config
import no.fintlabs.OtelConfig
import no.fintlabs.OtelInstrumentationConfig
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.AutoInstrumentation
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.LogDestination
import no.fintlabs.application.api.v1alpha1.Logging
import no.fintlabs.application.api.v1alpha1.toMetrics
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.PostgresUserDR
import no.fintlabs.common.pod.PodBuilder
import no.fintlabs.common.pod.PodBuilderContext
import no.fintlabs.common.utils.createObjectMeta
import no.fintlabs.common.utils.getLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import no.fintlabs.common.api.v1alpha1.Probe as FlaisProbe

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class DeploymentDR :
    CRUDKubernetesDependentResource<Deployment, FlaisApplication>(Deployment::class.java),
    KoinComponent {
    private val config: Config by inject()
    private val logger = getLogger()

    private val kafkaDR by inject<KafkaDR<FlaisApplication>>()
    private val postgresUserDR by inject<PostgresUserDR<FlaisApplication>>()
    private val onePasswordDR by inject<OnePasswordDR<FlaisApplication>>()
    private val podBuilder = PodBuilder.create(config, kafkaDR, postgresUserDR, onePasswordDR)

    override fun name() = "deployment"

    override fun desired(
        primary: FlaisApplication,
        context: Context<FlaisApplication>,
    ): Deployment {
        val podTemplate =
            podBuilder.build(
                primary,
                context,
                { builderContext -> cretePodMetadata(primary, builderContext) },
                { builderContext -> configurePodSpec(primary, builderContext, context) },
            )

        return Deployment().apply {
            metadata = createObjectMeta(primary)
            spec =
                DeploymentSpec().apply {
                    replicas = primary.spec.replicas
                    selector = LabelSelector(null, mapOf("app" to primary.metadata.name))
                    template = podTemplate
                    strategy = primary.spec.strategy
                }
        }
    }

    override fun handleUpdate(
        actual: Deployment,
        desired: Deployment,
        primary: FlaisApplication,
        context: Context<FlaisApplication>,
    ): Deployment {
        val kubernetesSerialization = context.client.kubernetesSerialization
        val desiredSelector =
            kubernetesSerialization.convertValue(desired.spec.selector, Map::class.java)
        val actualSelector = kubernetesSerialization.convertValue(actual.spec.selector, Map::class.java)
        val podSelectorMatch = desiredSelector == actualSelector

        if (podSelectorMatch) return super.handleUpdate(actual, desired, primary, context)

        logger.info("Pod selector does not match, recreating deployment ${actual.metadata.name}")
        handleDelete(primary, actual, context)
        return handleCreate(desired, primary, context)
    }

    private fun cretePodMetadata(
        primary: FlaisApplication,
        builderContext: PodBuilderContext,
    ) = createObjectMeta(primary).apply {
        annotations.putAll(builderContext.annotations)
        labels.putAll(builderContext.labels)

        annotations["kubectl.kubernetes.io/default-container"] = primary.metadata.name
        labels["observability.fintlabs.no/loki"] = lokiLabelEnabled(primary.spec.observability?.logging).toString()

        if (ebpfAutoInstrumentationEnabled(primary)) {
            labels["observability.fintlabs.no/ebpf-auto-instrumentation"] = "true"
        }
    }

    private fun configurePodSpec(
        primary: FlaisApplication,
        builderContext: PodBuilderContext,
        context: Context<FlaisApplication>,
    ) {
        createContainerEnv(primary, builderContext)
        configureOtel(primary, builderContext, context)
        builderContext.envFrom.addAll(primary.spec.envFrom)

        builderContext.containers +=
            Container().apply {
                name = primary.metadata.name
                image = primary.spec.image
                imagePullPolicy = primary.spec.imagePullPolicy
                resources = primary.spec.resources
                ports = createContainerPorts(primary)
                env = builderContext.getNormalizedEnv()
                envFrom = builderContext.envFrom
                volumeMounts = builderContext.volumeMounts
                startupProbe =
                    primary.spec.probes
                        ?.startup
                        ?.let { createPodProbe(it, primary.spec.port) }
                readinessProbe =
                    primary.spec.probes
                        ?.readiness
                        ?.let { createPodProbe(it, primary.spec.port) }
                livenessProbe =
                    primary.spec.probes
                        ?.liveness
                        ?.let { createPodProbe(it, primary.spec.port) }
            }
    }

    private fun configureOtel(
        primary: FlaisApplication,
        builderContext: PodBuilderContext,
        context: Context<FlaisApplication>,
    ) {
        val observability = primary.spec.observability ?: return
        val autoInstrumentation = observability.autoInstrumentation
        val loggingEnabled = observability.logging?.otel == true
        val metricsEnabled = observability.metrics?.otel == true
        val tracingEnabled = observability.tracing?.enabled == true

        if (!loggingEnabled && !metricsEnabled && !tracingEnabled) return

        val otelConfig = config.observability.otel
        if (!otelConfig.enabled) {
            error("OpenTelemetry is not supported for this cluster")
        }

        val existingEnv = builderContext.env.toList()
        builderContext.env.removeAll { it.name in OTEL_RESOURCE_ENV_NAMES }
        builderContext.env.addAll(
            listOf(
                EnvVar(OTEL_SERVICE_NAME, primary.metadata.name, null),
                EnvVar(
                    OTEL_RESOURCE_ATTRIBUTES,
                    otelAttributes(
                        primary,
                        namespace = primary.metadata.namespace ?: context.client.namespace ?: "default",
                        existingAttributes = existingEnv.firstOrNull { it.name == OTEL_RESOURCE_ATTRIBUTES }?.value,
                    ),
                    null,
                ),
                EnvVar(OTEL_LOGS_EXPORTER, exporterFor(loggingEnabled), null),
                EnvVar(OTEL_METRICS_EXPORTER, exporterFor(metricsEnabled), null),
                EnvVar(OTEL_TRACES_EXPORTER, exporterFor(tracingEnabled), null),
            ),
        )

        if (autoInstrumentation != null && autoInstrumentation.enabled) {
            if (autoInstrumentation.runtime.isNullOrBlank()) {
                error("Auto-instrumentation runtime must be specified when auto-instrumentation is enabled")
            }
            if (!config.observability.otel.autoInstrumentation.sdkInjectionEnabled) {
                error("SDK based on auto-instrumentation is not enabled in this cluster")
            }
            configureAutoInstrumentation(otelConfig, autoInstrumentation, primary, builderContext)
            return
        }

        if (config.observability.otel.autoInstrumentation.sdkInjectionEnabled) {
            configureSdkInjection(otelConfig, primary, builderContext)
        } else {
            val instrumentation = config.observability.otel.instrumentation
            if (instrumentation != null) {
                configureCollectorFallback(instrumentation, builderContext)
            }
        }
    }

    private fun configureAutoInstrumentation(
        otelConfig: OtelConfig,
        autoInstrumentation: AutoInstrumentation,
        primary: FlaisApplication,
        builderContext: PodBuilderContext,
    ) {
        val runtime = autoInstrumentation.runtime
        builderContext.annotations["instrumentation.opentelemetry.io/inject-$runtime"] =
            otelConfig.autoInstrumentation.instrumentationConfig
        builderContext.annotations["instrumentation.opentelemetry.io/container-names"] = primary.metadata.name
    }

    private fun configureSdkInjection(
        otelConfig: OtelConfig,
        primary: FlaisApplication,
        builderContext: PodBuilderContext,
    ) {
        builderContext.annotations["instrumentation.opentelemetry.io/inject-sdk"] =
            otelConfig.autoInstrumentation.instrumentationConfig
        builderContext.annotations["instrumentation.opentelemetry.io/container-names"] = primary.metadata.name
    }

    private fun configureCollectorFallback(
        instrumentation: OtelInstrumentationConfig,
        builderContext: PodBuilderContext,
    ) {
        builderContext.env.addAll(
            listOf(
                EnvVar(OTEL_EXPORTER_OTLP_ENDPOINT, instrumentation.collectorEndpoint, null),
                EnvVar(OTEL_EXPORTER_OTLP_PROTOCOL, instrumentation.collectorProtocol, null),
                EnvVar(OTEL_EXPORTER_OTLP_INSECURE, instrumentation.collectorInsecure.toString(), null),
            ),
        )
    }

    private fun createContainerPorts(primary: FlaisApplication): List<ContainerPort> {
        val ports =
            mutableListOf(
                ContainerPort().apply {
                    name = "http"
                    containerPort = primary.spec.port
                    protocol = "TCP"
                },
            )

        val metrics = primary.spec.observability?.metrics ?: primary.spec.prometheus.toMetrics()
        if (metrics.enabled && metrics.port.toInt() != primary.spec.port) {
            ports.add(
                ContainerPort().apply {
                    name = "metrics"
                    containerPort = metrics.port.toInt()
                    protocol = "TCP"
                },
            )
        }

        return ports
    }

    private fun createContainerEnv(
        primary: FlaisApplication,
        builderContext: PodBuilderContext,
    ) {
        primary.spec.url.basePath
            ?.takeIf { it.isNotBlank() }
            ?.let { basePath ->
                builderContext.env.add(EnvVar("spring.webflux.base-path", basePath, null))
                builderContext.env.add(EnvVar("spring.mvc.servlet.path", basePath, null))
            }
    }

    private fun createPodProbe(
        probe: FlaisProbe,
        appPort: Int,
    ) = Probe().apply {
        httpGet =
            HTTPGetAction().apply {
                path = probe.path.ensureLeadingSlash()
                port = probe.port ?: IntOrString(appPort)
            }
        initialDelaySeconds = probe.initialDelaySeconds.takeIfPositive()
        failureThreshold = probe.failureThreshold.takeIfPositive()
        periodSeconds = probe.periodSeconds.takeIfPositive()
        timeoutSeconds = probe.timeoutSeconds.takeIfPositive()
    }

    /**
     * Returns this value if > 0; otherwise null.
     *
     * Setting zero causes JOSDK to send zero (which Kubernetes then overrides to its default),
     * causing unnecessary updates. Returning null makes JOSDK omit the field so Kubernetes can apply
     * its default cleanly.
     */
    private fun Int?.takeIfPositive(): Int? = this?.takeIf { it > 0 }

    private fun String?.ensureLeadingSlash(): String =
        when {
            isNullOrBlank() -> "/"
            startsWith("/") -> this
            else -> "/$this"
        }

    private fun otelAttributes(
        primary: FlaisApplication,
        namespace: String,
        existingAttributes: String?,
    ): String {
        val attributes =
            listOfNotNull(
                "service.name=${primary.metadata.name}",
                "service.namespace=$namespace",
                primary.spec.observability?.logging?.let {
                    if (it.otel) "flais.backend.logs=${it.destination}" else null
                },
            ).toMutableList()

        existingAttributes
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.filter { attribute ->
                val name = attribute.substringBefore("=", missingDelimiterValue = "")
                name.isNotBlank() && name !in OTEL_RESERVED_ATTRIBUTES
            }?.let(attributes::addAll)

        return attributes.joinToString(",")
    }

    private fun exporterFor(enabled: Boolean) = if (enabled) "otlp" else "none"

    private fun lokiLabelEnabled(logging: Logging?): Boolean {
        if (logging == null) return true
        logging.loki?.let { return it }
        return (logging.enabled ?: true) && logging.destination == LogDestination.LOKI
    }

    private fun ebpfAutoInstrumentationEnabled(primary: FlaisApplication): Boolean {
        if (!config.observability.otel.autoInstrumentation.ebpfEnabled) return false
        if (config.observability.otel.autoInstrumentation.sdkInjectionEnabled) return false
        return primary.spec.observability
            ?.autoInstrumentation
            ?.enabled != true
    }

    companion object {
        private const val OTEL_EXPORTER_OTLP_ENDPOINT = "OTEL_EXPORTER_OTLP_ENDPOINT"
        private const val OTEL_EXPORTER_OTLP_PROTOCOL = "OTEL_EXPORTER_OTLP_PROTOCOL"
        private const val OTEL_EXPORTER_OTLP_INSECURE = "OTEL_EXPORTER_OTLP_INSECURE"

        private const val OTEL_SERVICE_NAME = "OTEL_SERVICE_NAME"
        private const val OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES"

        private const val OTEL_LOGS_EXPORTER = "OTEL_LOGS_EXPORTER"
        private const val OTEL_METRICS_EXPORTER = "OTEL_METRICS_EXPORTER"
        private const val OTEL_TRACES_EXPORTER = "OTEL_TRACES_EXPORTER"

        private val OTEL_RESOURCE_ENV_NAMES =
            setOf(
                OTEL_SERVICE_NAME,
                OTEL_RESOURCE_ATTRIBUTES,
                OTEL_LOGS_EXPORTER,
                OTEL_METRICS_EXPORTER,
                OTEL_TRACES_EXPORTER,
            )

        private val OTEL_RESERVED_ATTRIBUTES = setOf("service.name", "service.namespace", "flais.backend.logs")
    }
}
