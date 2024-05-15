
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

val kotlin_version: String by project
val mockk_version: String by project
val fabric8_version: String by project
val koin_version: String by project

plugins {
    kotlin("jvm")
    kotlin("kapt")
    application

    id("java")
    id("io.fabric8.java-generator")
}

group = "no.fintlabs"
version = "0.0.1-SNAPSHOT"

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
    mavenLocal()
    maven("https://repo.fintlabs.no/releases")
}

dependencies {
    implementation("io.fabric8:kubernetes-client:${fabric8_version}")
    implementation("io.fabric8:crd-generator-apt:${fabric8_version}")
    kapt("io.fabric8:crd-generator-apt:${fabric8_version}")
    implementation("io.javaoperatorsdk:operator-framework-core:4.8.3")
    implementation("io.insert-koin:koin-core:$koin_version")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("io.fabric8:kubernetes-server-mock:${fabric8_version}")
    testImplementation("io.fabric8:kube-api-test:${fabric8_version}")
    testImplementation("io.javaoperatorsdk:operator-framework-junit-5:4.8.3")
    testImplementation("io.insert-koin:koin-test:$koin_version")
    testImplementation("io.insert-koin:koin-test-junit5:$koin_version")
}

val copyResourceDefinition = tasks.register<Copy>("copyResourceDefinition") {
    from("${layout.buildDirectory}/classes/java/main/META-INF/fabric8/applications.fintlabs.no-v1.yml")
    into("$projectDir/kustomize/base")
}


testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            testType.set(TestSuiteType.UNIT_TEST)

            sources {
                kotlin {
                    srcDirs(layout.projectDirectory.dir("src/test/unit/kotlin"))
                }
            }

            targets {
                all {
                    testTask.configure {
                        useJUnitPlatform()
                        filter {
                            isFailOnNoMatchingTests = false
                        }
                        testLogging {
                            events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
                            exceptionFormat = TestExceptionFormat.FULL
                            showCauses = true
                            showExceptions = true
                            showStackTraces = true
                        }
                    }
                }
            }
        }

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

            targets {
                all {
                    testTask.configure {
                        useJUnitPlatform ()
                        filter {
                            isFailOnNoMatchingTests = false
                        }
                        testLogging {
                            events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
                            exceptionFormat = TestExceptionFormat.FULL
                            showCauses = true
                            showExceptions = true
                            showStackTraces = true
                        }
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("build") {
    finalizedBy(copyResourceDefinition)
}

tasks.named("clean") {
    doLast {
        delete("$projectDir/kustomize/base/applications.fintlabs.no-v1.yml")
    }
}

javaGen {
    source = file("src/main/resources/kubernetes")
    target = file(layout.buildDirectory.dir("generated/source/kubernetes/main"))
}