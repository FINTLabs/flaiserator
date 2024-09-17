import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.writeText

plugins {
    application

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
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
    mavenLocal()
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.bundles.fabric8)
    implementation(libs.operator.framework.core)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.logging)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.awaitility.kotlin)
    testImplementation(libs.operator.framework.junit5)
    testImplementation(libs.bundles.fabric8test)
    testImplementation(libs.bundles.koinTest)
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
    jar {
        archiveBaseName = "app"
        manifest {
            attributes["Main-Class"] = "no.fintlabs.ApplicationKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = layout.buildDirectory.file("libs/${it.name}").get().asFile
                if (!file.exists()) {
                    it.copyTo(file)
                }
            }
        }
    }

    withType<Wrapper> {
        gradleVersion = "8.7"
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    withType<KotlinCompile> {
        compilerOptions{
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xjsr305=strict")
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

    register("generateCrd") {
        project.dependencies {
            kapt(libs.fabric8.crd.generator.apt)
        }

        getByName("kaptKotlin").doLast {
            val objectMapper = ObjectMapper(YAMLFactory())
            val sourceDir = layout.buildDirectory.dir("tmp/kapt3/classes/main/META-INF/fabric8").get().asFile.toPath()
            val targetDir = layout.projectDirectory.dir("charts/flaiserator-crd/charts/crds/templates").asFile.toPath()

            if (targetDir.exists()) targetDir.toFile().deleteRecursively()

            Files.createDirectories(targetDir)
            Files.list(sourceDir).forEach { filePath ->
                if (filePath.toString().endsWith("-v1.yml")) {
                    val crdNode = objectMapper.readTree(filePath.toFile()) as ObjectNode
                    val metadataObject = crdNode.withObject("metadata")

                    // Ensure the annotations node exists
                    metadataObject.putNull("annotations")

                    var crdContent = objectMapper.writeValueAsString(crdNode)
                    crdContent = crdContent.replace(
                        "annotations: null",
                        """annotations:
                    |{{- with .Values.annotations }}
                    |{{- toYaml . | nindent 4 }}
                    |{{- end }}""".trimMargin()
                    )

                    val targetPath = targetDir.resolve(filePath.fileName)
                    Files.createFile(targetPath).writeText(crdContent, Charsets.UTF_8)
                }
            }
        }

        finalizedBy("kaptKotlin")
    }
}

fun fetchNumCores(): Int = Runtime.getRuntime().availableProcessors().let {
    if (System.getenv("CI") != null) it
    else it / 2
}.coerceAtLeast(1)