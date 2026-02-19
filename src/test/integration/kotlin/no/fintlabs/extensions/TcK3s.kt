package no.fintlabs.extensions

import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.ConcurrentHashMap

object TcK3s {
  const val CONTAINER_NAME_PREFIX = "flaiserator-test-"

  val kubernetesVersion = System.getenv("TEST_KUBERNETES_VERSION")?.let { "$it-k3s1" } ?: "latest"
  val kubernetesImage =  System.getenv("TEST_KUBERNETES_IMAGE") ?: "rancher/k3s"

  private val globalContainer: K3sContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    createContainer("global")
  }

  private val scopedContainers = ConcurrentHashMap<String, K3sContainer>()

  fun global(): K3sContainer = globalContainer

  fun scoped(key: String): K3sContainer =
    scopedContainers.computeIfAbsent(key) {
      createContainer(key)
    }

  fun stopScoped(key: String) {
    scopedContainers.remove(key)?.stop()
  }

  private fun createContainer(name: String): K3sContainer =
    K3sContainer(DockerImageName.parse("$kubernetesImage:$kubernetesVersion")).apply {
      withCreateContainerCmdModifier {
        it.withName("$CONTAINER_NAME_PREFIX$name")
      }
      withReuse(true)
      start()
    }

  private fun startGlobal() =
    global()

  private fun stopAll() {
    println("Stopping all test containers with prefix: $CONTAINER_NAME_PREFIX")

    val ids = dockerPsByPrefix(CONTAINER_NAME_PREFIX)

    if (ids.isEmpty()) {
      println("No containers found with prefix $CONTAINER_NAME_PREFIX")
      return
    }

    runDocker("rm", "-f", *ids.toTypedArray())
  }

  private fun dockerPsByPrefix(prefix: String): List<String> =
    runDocker(
      "ps", "-a",
      "--filter", "name=^$prefix",
      "--format", "{{.ID}}",
      captureOutput = true
    ).filter { it.isNotBlank() }

  private fun runDocker(
    vararg args: String,
    captureOutput: Boolean = false,
    ignoreError: Boolean = false,
  ): List<String> {

    val builder = ProcessBuilder("docker", *args)

    if (!captureOutput) {
      builder.inheritIO()
    } else {
      builder.redirectErrorStream(true)
    }

    val process = builder.start()

    val output = if (captureOutput) {
      process.inputStream.bufferedReader().readLines()
    } else {
      emptyList()
    }

    val exit = process.waitFor()

    if (exit != 0 && !ignoreError) {
      error("Docker command failed: docker ${args.joinToString(" ")}")
    }

    return output
  }

  @JvmStatic
  fun main(args: Array<String>) {
    when (args.firstOrNull()) {
      "start" -> startGlobal()
      "stop" -> stopAll()
      else -> error("Usage: start | stop")
    }
  }
}