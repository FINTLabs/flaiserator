package no.fintlabs.application.crd

import spock.lang.Specification

class PrometheusSpec extends Specification {
    def "Creating a prometheus object with default values only shold contain 3 annotations and scraping should be turned of"() {
        given:
        def prometheus = Prometheus.builder().build()

        when:
        def annotations = prometheus.getPrometheusAnnotations()

        then:
        annotations.size() == 3
        annotations.get("prometheus.io/scrape") == "false"
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
