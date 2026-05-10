plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))

    implementation(platform(libs.jackson.bom)) {
        because("Override Fabric8 transitive Jackson version to avoid WS-2026-0003 in the bundled version")
    }
    implementation(libs.fabric8.generator.api)
    implementation(libs.fabric8.generator.collector)
}
