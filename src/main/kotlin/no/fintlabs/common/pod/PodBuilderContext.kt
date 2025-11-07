package no.fintlabs.common.pod

import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Volume
import io.fabric8.kubernetes.api.model.VolumeMount

data class PodBuilderContext(
  val annotations: MutableMap<String, String> = mutableMapOf(),
  val labels: MutableMap<String, String> = mutableMapOf(),
  val env: MutableList<EnvVar> = mutableListOf(),
  val envFrom: MutableList<EnvFromSource> = mutableListOf(),
  val containers: MutableList<Container> = mutableListOf(),
  val initContainers: MutableList<Container> = mutableListOf(),
  val volumes: MutableList<Volume> = mutableListOf(),
  val volumeMounts: MutableList<VolumeMount> = mutableListOf()
)