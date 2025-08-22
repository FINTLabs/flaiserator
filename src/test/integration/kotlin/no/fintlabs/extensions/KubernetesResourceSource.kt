package no.fintlabs.extensions

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class KubernetesResourceSource(private val file: File) {
  fun open(): InputStream? =
      try {
        file.inputStream()
      } catch (e: IOException) {
        null
      }

  fun ext() = file.extension

  companion object {
    private const val SUPPORTED_FORMATS = "yaml|yml"

    private fun fromPath(path: Path): List<KubernetesResourceSource> =
        when {
          Files.isDirectory(path) ->
              path.toFile().listFiles()?.flatMap { fromPath(it.toPath()) } ?: emptyList()
          else -> listOf(KubernetesResourceSource(path.toFile()))
        }

    fun fromResources(resources: List<String>): List<KubernetesResourceSource> =
        resources
            .mapNotNull { resource ->
              Companion::class
                  .java
                  .classLoader
                  .getResource(resource)
                  ?.toURI()
                  ?.let(Paths::get)
                  ?.let(::fromPath)
            }
            .flatten()
            .filter { it.ext() in SUPPORTED_FORMATS }
  }
}
