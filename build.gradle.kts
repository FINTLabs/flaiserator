import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  application
  alias(libs.plugins.fabric8.generator)
  alias(libs.plugins.spotless)
}

group = "no.fintlabs"

sourceSets {
  main { java { srcDirs(layout.buildDirectory.dir("generated/source/kubernetes/main")) } }
}

application { mainClass.set("no.fintlabs.ApplicationKt") }

repositories { mavenCentral() }

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
  testImplementation(platform(libs.testcontainers.bom))
  testImplementation(libs.testcontainers.k3s)
  testImplementation(libs.bundles.fabric8test)
  testImplementation(libs.bundles.koinTest)
  testImplementation(libs.logcapturer)
}

testing {
  suites {
    @Suppress("UnstableApiUsage")
    withType<JvmTestSuite> {
      useJUnitJupiter()
    }

    @Suppress("UnstableApiUsage", "unused")
    val test by
      getting(JvmTestSuite::class) {
        sources {
          kotlin { srcDirs(layout.projectDirectory.dir("src/test/unit/kotlin")) }
          resources { setSrcDirs(listOf("src/test/unit/resources")) }
        }
      }

    @Suppress("UnstableApiUsage", "unused")
    val integrationTest by
      registering(JvmTestSuite::class) {
        sources {
          kotlin { srcDirs(layout.projectDirectory.dir("src/test/integration/kotlin")) }
          resources { setSrcDirs(listOf("src/test/integration/resources")) }
        }

        dependencies {
          implementation(project())
        }

        configurations {
          named("${name}Implementation").configure {
            this.extendsFrom(configurations.testImplementation.get())
          }
        }
      }
  }
}

tasks {
  withType<Wrapper> { gradleVersion = "9.0" }

  java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

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
        configurations.runtimeClasspath.get().joinToString(separator = " ") { it.name }
    }

    val configuration = configurations.runtimeClasspath.get().map { it.toPath().toFile() }
    val buildDirectory = layout.buildDirectory

    doLast {
      configuration.forEach {
        val file = buildDirectory.file("libs/${it.name}").get().asFile
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

  compileKotlin { dependsOn(crd2java) }

  register<GenerateCrdsTask>("generateCrds") {
    description =
      "Generate CRDs from the compiled custom resource class `no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd`"
    group = "crd"

    sourceSet = sourceSets.main
    includePackages = listOf("no.fintlabs.application.api", "no.fintlabs.job.api")
    targetDirectory =
      project.layout.projectDirectory.dir("charts/flaiserator-crd/charts/crds/templates")

    dependsOn(compileJava, compileKotlin)
  }

  withType<Test> {
    maxParallelForks = fetchNumCores()

    testLogging {
      events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
  }

  val tcEnv = mapOf(
    "TESTCONTAINERS_REUSE_ENABLE" to "true",
    "TESTCONTAINERS_RYUK_DISABLED" to "true",
  )

  fun JavaExec.configureTcK3s(vararg tcArgs: String) {
    mainClass = "no.fintlabs.extensions.TcK3s"
    classpath = sourceSets["integrationTest"].runtimeClasspath
    args(*tcArgs)
    environment(tcEnv)
  }

  val startGlobalK3s by registering(JavaExec::class) {
    configureTcK3s("start")
  }

  val stopK3s by registering(JavaExec::class) {
    configureTcK3s("stop")
  }

  named<Test>("integrationTest") {
    dependsOn(startGlobalK3s)
    finalizedBy(stopK3s)
    environment(tcEnv)
  }
}

spotless {
  kotlin {
    ktfmt("0.57").metaStyle()
  }
}


fun fetchNumCores(): Int =
  Runtime.getRuntime()
    .availableProcessors()
    .let {
      if (System.getenv("CI") != null) {
        it
      } else {
        it / 2
      }
    }
    .coerceAtLeast(1)
