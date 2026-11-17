package no.fintlabs.system

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import no.fintlabs.extensions.TcK3s
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.MountableFile
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText

class SystemTestCluster {
    private val root = Path.of(System.getProperty("systemTest.projectRoot", ".")).toAbsolutePath()
    private val workdir = Files.createTempDirectory("flaiserator-system-test-")
    private val kubeconfig = workdir.resolve("kubeconfig")
    private val clusterKey = "system-${UUID.randomUUID().toString().take(8)}"
    private val clusterName = "${TcK3s.CONTAINER_NAME_PREFIX}$clusterKey"
    private val artifacts = Files.createDirectories(root.resolve("build/system-test/$clusterName"))
    private var commandNumber = 0
    private var k3s: K3sContainer? = null
    lateinit var client: KubernetesClient
        private set

    fun start() {
        command("docker", "info")
        command("docker", "build", "-t", "flaiserator:system-test", ".")
        for (target in listOf("application", "job")) {
            command(
                "docker",
                "build",
                "--target",
                target,
                "-t",
                "flaiserator-test-$target:system-test",
                "src/test/system/resources/workloads",
            )
        }

        val archive = workdir.resolve("images.tar")
        command(
            "docker",
            "save",
            "--output",
            archive.toString(),
            "flaiserator:system-test",
            "flaiserator-test-application:system-test",
            "flaiserator-test-job:system-test",
        )
        val container = TcK3s.scoped(clusterKey, reuse = false).also { k3s = it }
        Files.writeString(kubeconfig, container.kubeConfigYaml)
        container.copyFileToContainer(MountableFile.forHostPath(archive), "/tmp/system-test-images.tar")
        val imported = container.execInContainer("ctr", "--namespace", "k8s.io", "images", "import", "/tmp/system-test-images.tar")
        Files.writeString(artifacts.resolve("image-import.log"), imported.stdout + imported.stderr)
        check(imported.exitCode == 0) { "Could not import test images into K3s: ${imported.stderr}" }
        client =
            KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubeconfig.readText()))
                .withKubernetesSerialization(KubernetesSerialization(jacksonObjectMapper(), true))
                .build()

        // Install the real API definitions watched by the operator, without external controllers.
        kubectl("apply", "--server-side", "-f", "src/main/resources/kubernetes")
        kubectl("wait", "--for=condition=Established", "crd", "--all", "--timeout=180s")
        val crdChart = workdir.resolve("flaiserator-crd")
        check(root.resolve("charts/flaiserator-crd").toFile().copyRecursively(crdChart.toFile()))
        command("helm", "dependency", "build", crdChart.toString(), "--skip-refresh")
        command(
            "helm",
            "install",
            "flaiserator-crd",
            crdChart.toString(),
            "--namespace",
            "flais-system",
            "--create-namespace",
            "--wait",
            "--timeout",
            "180s",
        )
        kubectl(
            "wait",
            "--for=condition=Established",
            "crd/applications.fintlabs.no",
            "crd/jobs.fintlabs.no",
            "--timeout=180s",
        )
        command(
            "helm",
            "install",
            "flaiserator",
            "charts/flaiserator",
            "--namespace",
            "flais-system",
            "--wait",
            "--timeout",
            "180s",
            "--set",
            "image.repository=flaiserator",
            "--set",
            "image.tag=system-test",
            "--set",
            "image.pullPolicy=Never",
        )
    }

    fun kubectl(vararg arguments: String): String = command("kubectl", "--request-timeout=30s", *arguments)

    fun close() {
        try {
            k3s?.let { container ->
                runCatching { Files.writeString(artifacts.resolve("k3s.log"), container.logs) }
            }
            if (::client.isInitialized) {
                // Preserve diagnostics even if setup or an assertion failed.
                val diagnostics =
                    listOf(
                        listOf("get", "pods", "-A", "-o", "wide"),
                        listOf("get", "events", "-A", "--sort-by=.lastTimestamp"),
                        listOf("-n", "flais-system", "logs", "deployment/flaiserator", "--all-containers", "--tail=1000"),
                        listOf("-n", "system-test-app", "get", "applications.fintlabs.no", "-o", "yaml"),
                        listOf("-n", "system-test-job", "get", "jobs.fintlabs.no", "-o", "yaml"),
                        listOf("describe", "pods", "-A"),
                    )
                diagnostics.forEach { args ->
                    runCatching { kubectl(*args.toTypedArray()) }.onFailure { System.err.println(it.message) }
                }
                println("System test diagnostics: $artifacts")
            }
        } finally {
            try {
                if (::client.isInitialized) client.close()
            } finally {
                try {
                    TcK3s.stopScoped(clusterKey)
                } finally {
                    check(workdir.toFile().deleteRecursively()) { "Could not delete $workdir" }
                }
            }
        }
    }

    private fun command(vararg arguments: String): String {
        val output = artifacts.resolve("${++commandNumber}-${arguments.first()}.log")
        println("Running: ${arguments.joinToString(" ")}")
        val process =
            ProcessBuilder(*arguments)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .apply {
                    environment()["KUBECONFIG"] = kubeconfig.toString()
                }.start()
        try {
            check(process.waitFor(Duration.ofMinutes(5).toMillis(), TimeUnit.MILLISECONDS)) {
                "Command timed out: ${arguments.joinToString(" ")}; output: $output"
            }
            val text = output.readText().trim()
            check(process.exitValue() == 0) { "Command failed: ${arguments.joinToString(" ")}\n$text" }
            return text
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(10, TimeUnit.SECONDS)
            }
        }
    }
}
