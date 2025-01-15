plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))

    implementation(libs.fabric8.generator.api)
    implementation(libs.fabric8.generator.collector)
}