package no.fintlabs.system

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.extensions.TcK3s
import no.fintlabs.job.api.v1alpha1.FlaisJob
import org.testcontainers.k3s.K3sContainer
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

class SystemTestCluster {
    private val root = Path.of(System.getProperty("systemTest.projectRoot", ".")).toAbsolutePath()
    private val workdir = Files.createTempDirectory("flaiserator-system-test-")
    private val clusterKey = "system-${UUID.randomUUID().toString().take(8)}"
    private val artifacts = Files.createDirectories(root.resolve("build/system-test/$clusterKey"))
    private var k3s: K3sContainer? = null
    lateinit var client: KubernetesClient
        private set

    fun start() {
        val container = TcK3s.scoped(clusterKey, reuse = false).also { k3s = it }
        client =
            KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(container.kubeConfigYaml))
                .withKubernetesSerialization(KubernetesSerialization(jacksonObjectMapper(), true))
                .build()
        SystemTestImages.buildAndImport(root, workdir, container)
        installExternalCrds()

        val kubeconfig = Files.writeString(workdir.resolve("kubeconfig"), container.kubeConfigYaml)
        val helm = Helm(kubeconfig, artifacts)
        val crdChart = workdir.resolve("flaiserator-crd")
        check(root.resolve("charts/flaiserator-crd").toFile().copyRecursively(crdChart.toFile()))
        helm.buildDependencies(crdChart)
        helm.install("flaiserator-crd", crdChart, "flais-system")
        waitForCrds(listOf("applications.fintlabs.no", "jobs.fintlabs.no"))
        helm.install(
            "flaiserator",
            root.resolve("charts/flaiserator"),
            "flais-system",
            mapOf("image.repository" to "flaiserator", "image.tag" to "system-test", "image.pullPolicy" to "Never"),
        )
    }

    private fun installExternalCrds() {
        // These APIs are watched by the operator; their controllers are unnecessary for these tests.
        Files.list(root.resolve("src/main/resources/kubernetes")).use { definitions ->
            definitions.filter { it.toString().endsWith(".yaml") || it.toString().endsWith(".yml") }.forEach { path ->
                val resources = Files.newInputStream(path).use { client.load(it).serverSideApply() }
                waitForCrds(resources.map { it.metadata.name })
            }
        }
    }

    private fun waitForCrds(names: List<String>) {
        names.forEach { name ->
            client.apiextensions().v1().customResourceDefinitions().withName(name).waitUntilCondition(
                { crd -> crd?.status?.conditions?.any { it.type == "Established" && it.status == "True" } == true },
                3,
                TimeUnit.MINUTES,
            )
        }
    }

    fun close() {
        try {
            captureDiagnostics()
        } finally {
            try {
                if (::client.isInitialized) client.close()
            } finally {
                try {
                    TcK3s.stopScoped(clusterKey)
                } finally {
                    check(workdir.toFile().deleteRecursively()) { "Could not delete $workdir" }
                }
            }
        }
    }

    private fun captureDiagnostics() {
        k3s?.let { container -> save("k3s.log") { container.logs } }
        if (!::client.isInitialized) return
        save("pods.yaml") { client.pods().inAnyNamespace().list() }
        save("events.yaml") {
            client
                .v1()
                .events()
                .inAnyNamespace()
                .list()
        }
        save("applications.yaml") { client.resources(FlaisApplication::class.java).inAnyNamespace().list() }
        save("jobs.yaml") { client.resources(FlaisJob::class.java).inAnyNamespace().list() }
        runCatching {
            client.pods().inNamespace("flais-system").withLabel("app.kubernetes.io/name", "flaiserator").list().items.forEach { pod ->
                save("${pod.metadata.name}.log") {
                    client
                        .pods()
                        .inNamespace("flais-system")
                        .withName(pod.metadata.name)
                        .tailingLines(1000)
                        .log
                }
            }
        }.onFailure { System.err.println("Could not collect operator logs: ${it.message}") }
        println("System test diagnostics: $artifacts")
    }

    private fun save(
        name: String,
        content: () -> Any,
    ) {
        runCatching {
            val value = content()
            Files.writeString(artifacts.resolve(name), if (value is String) value else client.kubernetesSerialization.asYaml(value))
        }.onFailure { System.err.println("Could not save $name: ${it.message}") }
    }
}
