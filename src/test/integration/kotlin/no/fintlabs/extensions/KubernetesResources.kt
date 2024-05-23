package no.fintlabs.extensions

@Retention(AnnotationRetention.RUNTIME)
annotation class KubernetesResources(vararg val paths: String)
