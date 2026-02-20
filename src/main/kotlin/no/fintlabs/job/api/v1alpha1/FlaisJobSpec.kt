package no.fintlabs.job.api.v1alpha1

import io.fabric8.generator.annotation.ValidationRule
import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirements
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import no.fintlabs.common.WithKafka
import no.fintlabs.common.WithOnePassword
import no.fintlabs.common.WithPostgres
import no.fintlabs.common.api.v1alpha1.Database
import no.fintlabs.common.api.v1alpha1.FlaisResourceSpec
import no.fintlabs.common.api.v1alpha1.Kafka
import no.fintlabs.common.api.v1alpha1.OnePassword

data class FlaisJobSpec(
    override val orgId: String = "",
    override val image: String = "",
    val schedule: String? = null,
    @ValidationRule(
        "self in ['IfNotPresent', 'Always', 'Never']",
        message = "Invalid imagePullPolicy, must be one of IfNotPresent, Always, Never",
    )
    override val imagePullPolicy: String? = null,
    override val imagePullSecrets: List<String> = emptyList(),
    override val env: List<EnvVar> = emptyList(),
    override val envFrom: List<EnvFromSource> = emptyList(),
    override val resources: ResourceRequirements =
        ResourceRequirementsBuilder()
            .addToRequests("cpu", Quantity("200m"))
            .addToRequests("memory", Quantity("256Mi"))
            .addToLimits("memory", Quantity("512Mi"))
            .build(),
    override val onePassword: OnePassword? = null,
    override val kafka: Kafka = Kafka(),
    override val observability: JobObservability? = null,
    override val database: Database = Database(),
    val timezone: String = "Europe/Oslo",
    val activeDeadlineSeconds: Long? = null,
    val backoffLimit: Int = 6,
    val ttlSecondsAfterFinished: Int? = null,
    val successfulJobsHistoryLimit: Int = 6,
    val failedJobsHistoryLimit: Int = 6,
    @ValidationRule(
        "self in ['Allow', 'Forbid', 'Replace']",
        message = "Invalid concurrency policy, must be one of Allow, Forbid, Replace",
    )
    val concurrencyPolicy: String = "Allow",
    @ValidationRule(
        "self in ['Never', 'OnFailure']",
        message = "Invalid restartPolicy, must be one of Never, OnFailure",
    )
    val restartPolicy: String = "Never",
) : FlaisResourceSpec, WithOnePassword, WithKafka, WithPostgres
