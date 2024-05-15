package no.fintlabs.operator.application.api

data class Prometheus(
    val enabled: Boolean = true,
    val path: String = "/actuator/prometheus",
    val port: String = "8080"
) {

    fun getPrometheusAnnotations(): Map<String, String> {
        val annotations: MutableMap<String, String> = HashMap()

        if (enabled) {
            annotations["prometheus.io/scrape"] = enabled.toString()
            annotations["prometheus.io/port"] = port
            annotations["prometheus.io/path"] = path
        }
        return annotations
    }
}