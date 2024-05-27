rootProject.name = "flaiserator"

pluginManagement {
    val kotlinVersion: String by settings
    val fabric8Version: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        id("io.fabric8.java-generator") version fabric8Version
    }
}