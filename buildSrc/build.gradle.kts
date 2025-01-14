
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.0"
    `kotlin-dsl`
}

group = "no.fintlabs.fintlabs"
version = "unspecified"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.fabric8.generator.api)
    implementation(libs.fabric8.generator.collector)
}

tasks {
    withType<Wrapper> {
        gradleVersion = "8.12"
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}