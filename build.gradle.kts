
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by project
val mockkVersion: String by project
val fabric8Version: String by project
val koinVersion: String by project
val operatorSdkVersion: String by project
val awaitilityVersion: String by project
val hopliteVersion: String by project
val logbackVersion: String by project


plugins {
    application
    kotlin("jvm")
    kotlin("kapt")

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
}

dependencies {
    implementation("io.fabric8:kubernetes-client:$fabric8Version")
    implementation("io.fabric8:crd-generator-apt:$fabric8Version")
    implementation("io.javaoperatorsdk:operator-framework-core:$operatorSdkVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")


    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.fabric8:kubernetes-server-mock:${fabric8Version}")
    testImplementation("io.fabric8:kube-api-test:${fabric8Version}")
    testImplementation("io.javaoperatorsdk:operator-framework-junit-5:$operatorSdkVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")
    testImplementation("org.awaitility:awaitility-kotlin:$awaitilityVersion")
}




testing {
    suites {
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
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

tasks {
    withType<Wrapper> {
        gradleVersion = "8.7"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_21.toString()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    compileKotlin {
        dependsOn(crd2java)
    }

    withType<Test> {
        useJUnitPlatform()
    }

    register("generateCrd") {
        project.dependencies {
            kapt("io.fabric8:crd-generator-apt:$fabric8Version")
        }

        getByName("kaptKotlin").doLast {
            copy {
                from(layout.buildDirectory.dir("tmp/kapt3/classes/main/META-INF/fabric8"))
                into(layout.buildDirectory.dir("generated/kubernetes"))
            }
        }

        finalizedBy("kaptKotlin")
    }
}

javaGen {
    source = file(layout.projectDirectory.dir("src/main/resources/kubernetes"))
    target = file(layout.buildDirectory.dir("generated/source/kubernetes/main"))
}