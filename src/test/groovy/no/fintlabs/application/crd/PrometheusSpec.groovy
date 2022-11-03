package no.fintlabs.application.crd

import spock.lang.Specification

class PrometheusSpec extends Specification {
    def "Creating a prometheus object with default values only should contain 0 annotations"() {
        given:
        def prometheus = new Prometheus()

        when:
        def annotations = prometheus.getPrometheusAnnotations()

        then:
        annotations.size() == 0
    }

    def "Creating a prometheus object with custom values the values should be reflected in the annotations"() {
        given:
        def prometheus = Prometheus.builder()
        .enabled(true)
        .path("/path")
        .port("80")
                .build()

        when:
        def annotations = prometheus.getPrometheusAnnotations()

        then:
        annotations.size() == 3
        annotations.get("prometheus.io/scrape") == "true"
        annotations.get("prometheus.io/path") == "/path"
        annotations.get("prometheus.io/port") == "80"
    }
}
