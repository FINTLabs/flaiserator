package no.fintlabs.system

import org.testcontainers.DockerClientFactory
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.MountableFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object SystemTestImages {
    fun buildAndImport(
        root: Path,
        workdir: Path,
        container: K3sContainer,
    ) {
        val images =
            listOf(
                ImageFromDockerfile("flaiserator:system-test")
                    .withFileFromPath("Dockerfile", root.resolve("Dockerfile"))
                    .withFileFromPath("build/libs", root.resolve("build/libs")),
                workload("application"),
                workload("job"),
            ).map { it.get(5, TimeUnit.MINUTES) }
        val archive = workdir.resolve("images.tar")
        DockerClientFactory.instance().client().saveImagesCmd().use { export ->
            images.forEach { export.withImage(it.substringBeforeLast(':'), it.substringAfterLast(':')) }
            export.exec().use { Files.copy(it, archive) }
        }
        container.copyFileToContainer(MountableFile.forHostPath(archive), "/tmp/system-test-images.tar")
        val imported = container.execInContainer("ctr", "--namespace", "k8s.io", "images", "import", "/tmp/system-test-images.tar")
        check(imported.exitCode == 0) { "Could not import test images into K3s: ${imported.stderr}" }
    }

    private fun workload(target: String): ImageFromDockerfile =
        ImageFromDockerfile("flaiserator-test-$target:system-test")
            .withFileFromClasspath("Dockerfile", "workloads/Dockerfile")
            .withTarget(target)
}
