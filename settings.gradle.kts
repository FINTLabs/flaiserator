rootProject.name = "flaiserator"

pluginManagement {
    val kotlin_version: String by settings
    val fabric8_version: String by settings
    plugins {
        kotlin("jvm") version kotlin_version
        kotlin("kapt") version kotlin_version
        id("io.fabric8.java-generator") version fabric8_version
    }
}