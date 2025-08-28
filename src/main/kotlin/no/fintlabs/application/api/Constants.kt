package no.fintlabs.application.api

const val DEPLOYMENT_CORRELATION_ID_ANNOTATION = "fintlabs.no/deployment-correlation-id"

val MANAGED_BY_FLAISERATOR_LABEL = "app.kubernetes.io/managed-by" to "flaiserator"
const val MANAGED_BY_FLAISERATOR_SELECTOR = "app.kubernetes.io/managed-by=flaiserator"

const val ORG_ID_LABEL = "fintlabs.no/org-id"
const val TEAM_LABEL = "fintlabs.no/team"

const val LOKI_LOGGING_LABEL = "observability.fintlabs.no/loki"
