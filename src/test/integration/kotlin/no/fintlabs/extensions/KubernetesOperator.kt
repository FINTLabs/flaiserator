package no.fintlabs.extensions

annotation class KubernetesOperator(
    val explicitStart: Boolean = false,
    val registerReconcilers: Boolean = true,
)
