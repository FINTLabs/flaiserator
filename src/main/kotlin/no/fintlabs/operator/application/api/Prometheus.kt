package no.fintlabs.operator.application.api

data class Prometheus(
    var enabled: Boolean = true,
    var path: String = "/actuator/prometheus",
    var port: String = "8080"
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