import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    application
    kotlin("jvm")
    alias(libs.plugins.fabric8.generator)
}

group = "no.fintlabs"

sourceSets {
    main {
        java {
            srcDirs(layout.buildDirectory.dir("generated/source/kubernetes/main"))
        }
    }
}

application {
    mainClass.set("no.fintlabs.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.bundles.fabric8)
    implementation(libs.bundles.operator)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.logging)
    implementation(libs.jackson.module.kotlin)
    implementation(platform(libs.http4k.bom))
    implementation(libs.bundles.http4k)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.awaitility.kotlin)
    testImplementation(libs.operator.framework.junit5)
    testImplementation(libs.testcontainers.k3s)
    testImplementation(libs.bundles.fabric8test)
    testImplementation(libs.bundles.koinTest)
    testImplementation(libs.bundles.logunit)
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        val test by getting(JvmTestSuite::class) {
            testType.set(TestSuiteType.UNIT_TEST)
            sources {
                kotlin {
                    srcDirs(layout.projectDirectory.dir("src/test/unit/kotlin"))
                }
                resources {
                    setSrcDirs(listOf("src/test/unit/resources"))
                }
            }
        }

        @Suppress("UnstableApiUsage")
        val integrationTest by registering(JvmTestSuite::class) {
            testType.set(TestSuiteType.INTEGRATION_TEST)
            sources {
                kotlin {
                    srcDirs(layout.projectDirectory.dir("src/test/integration/kotlin"))
                }
                resources {
                    setSrcDirs(listOf("src/test/integration/resources"))
                }
            }

            dependencies {
                // We can replace direct dependency on test's runtimeClasspath with implementation(project())
                // once https://github.com/gradle/gradle/issues/25269 is resolved
                implementation(sourceSets.test.get().runtimeClasspath)
                implementation(sourceSets.test.get().output)
            }
        }
    }
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

    jar {
        archiveBaseName = "app"
        manifest {
            attributes["Main-Class"] = "no.fintlabs.ApplicationKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }

        val configuration = configurations.runtimeClasspath.get().map {
            it.toPath().toFile()
        }
        val buildDirectory = layout.buildDirectory

        doLast {
            configuration.forEach {
                val file = buildDirectory
                        .file("libs/${it.name}")
                        .get()
                        .asFile
                if (!file.exists()) {
                    it.copyTo(file)
                }
            }
        }
    }

    javaGen {
        source = file(layout.projectDirectory.dir("src/main/resources/kubernetes"))
        target = file(layout.buildDirectory.dir("generated/source/kubernetes/main"))
    }

    compileKotlin {
        dependsOn(crd2java)
    }

    withType<Test> {
        useJUnitPlatform()

        maxParallelForks = fetchNumCores()
        forkEvery = 1

        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    register<GenerateCrdsTask>("generateCrds") {
        description = "Generate CRDs from the compiled custom resource class `no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd`"
        group = "crd"

        sourceSet = sourceSets["main"]
        includePackages = listOf("no.fintlabs.application.api")
        targetDirectory = project.layout.projectDirectory.dir("charts/flaiserator-crd/charts/crds/templates")

        dependsOn(compileJava, compileKotlin)
    }
}

fun fetchNumCores(): Int = Runtime.getRuntime().availableProcessors().let {
    if (System.getenv("CI") != null) it
    else it / 2
}.coerceAtLeast(1)