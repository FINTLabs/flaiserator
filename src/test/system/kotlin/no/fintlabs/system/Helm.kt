package no.fintlabs.system

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText

/** Run the real Helm CLI: chart installation is part of the behavior under test. */
class Helm(
    private val kubeconfig: Path,
    private val artifacts: Path,
) {
    private var commandNumber = 0

    fun buildDependencies(chart: Path) {
        run(listOf("dependency", "build", chart.toString(), "--skip-refresh"))
    }

    fun install(
        release: String,
        chart: Path,
        namespace: String,
        values: Map<String, String> = emptyMap(),
    ) {
        run(
            listOf(
                "install",
                release,
                chart.toString(),
                "--namespace",
                namespace,
                "--create-namespace",
                "--wait",
                "--timeout",
                "180s",
            ) + values.flatMap { (key, value) -> listOf("--set-string", "$key=$value") },
        )
    }

    private fun run(arguments: List<String>) {
        val output = artifacts.resolve("helm-${++commandNumber}.log")
        val process =
            ProcessBuilder(listOf("helm", "--kubeconfig", kubeconfig.toString()) + arguments)
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start()
        try {
            check(process.waitFor(5, TimeUnit.MINUTES)) { "Helm timed out: $arguments; output: $output" }
            check(process.exitValue() == 0) { "Helm failed: $arguments\n${output.readText()}" }
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(10, TimeUnit.SECONDS)
            }
        }
    }
}
