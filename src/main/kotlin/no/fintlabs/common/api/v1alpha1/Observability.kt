package no.fintlabs.common.api.v1alpha1

import no.fintlabs.application.api.v1alpha1.Logging

interface Observability {
  val logging: Logging?
}
