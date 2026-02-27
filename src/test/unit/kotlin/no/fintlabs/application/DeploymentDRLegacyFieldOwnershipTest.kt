package no.fintlabs.application

import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.PodSpec
import io.fabric8.kubernetes.api.model.PodTemplateSpec
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeploymentDRLegacyFieldOwnershipTest {
  @Test
  fun `should recreate when legacy manager owns any stale env vars`() {
    val actual =
        deployment(
            envNames = setOf("JAVA_TOOL_OPTIONS", "MY_STALE_ENV"),
            managers = setOf("flaisapplicationreconciler"),
        )
    val desired =
        deployment(envNames = setOf("JAVA_TOOL_OPTIONS"), managers = setOf("applicationreconciler"))

    val staleEnvVarNames = DeploymentDR.staleLegacyEnvVarNames(actual, desired)
    assertTrue(DeploymentDR.shouldRecreateForLegacyFieldOwnership(actual, staleEnvVarNames))
  }

  @Test
  fun `should not recreate when legacy manager is absent`() {
    val actual =
        deployment(
            envNames = setOf("JAVA_TOOL_OPTIONS", "MY_STALE_ENV"),
            managers = setOf("applicationreconciler"),
        )
    val desired =
        deployment(envNames = setOf("JAVA_TOOL_OPTIONS"), managers = setOf("applicationreconciler"))

    val staleEnvVarNames = DeploymentDR.staleLegacyEnvVarNames(actual, desired)
    assertFalse(DeploymentDR.shouldRecreateForLegacyFieldOwnership(actual, staleEnvVarNames))
  }

  @Test
  fun `should not recreate when stale env vars are absent`() {
    val actual =
        deployment(
            envNames = setOf("JAVA_TOOL_OPTIONS", "MY_ENV"),
            managers = setOf("flaisapplicationreconciler"),
        )
    val desired =
        deployment(
            envNames = setOf("JAVA_TOOL_OPTIONS", "MY_ENV"),
            managers = setOf("applicationreconciler"),
        )

    val staleEnvVarNames = DeploymentDR.staleLegacyEnvVarNames(actual, desired)
    assertFalse(DeploymentDR.shouldRecreateForLegacyFieldOwnership(actual, staleEnvVarNames))
  }

  @Test
  fun `should include container name in stale env var identity`() {
    val actual =
        deployment(
            envNames = setOf("JAVA_TOOL_OPTIONS", "MY_STALE_ENV"),
            managers = setOf("flaisapplicationreconciler"),
            containerName = "my-app",
        )
    val desired =
        deployment(
            envNames = setOf("JAVA_TOOL_OPTIONS"),
            managers = setOf("applicationreconciler"),
            containerName = "my-app",
        )

    val staleEnvVarNames = DeploymentDR.staleLegacyEnvVarNames(actual, desired)
    assertTrue("my-app:MY_STALE_ENV" in staleEnvVarNames)
  }

  private fun deployment(
      envNames: Set<String>,
      managers: Set<String>,
      containerName: String = "test",
  ): Deployment =
      Deployment().apply {
        metadata =
            ObjectMeta().apply {
              name = "test"
              managedFields =
                  managers
                      .map { manager ->
                        ManagedFieldsEntry().apply {
                          this.manager = manager
                          operation = "Apply"
                        }
                      }
                      .toMutableList()
            }
        spec =
            DeploymentSpec().apply {
              template =
                  PodTemplateSpec().apply {
                    spec =
                        PodSpec().apply {
                          containers =
                              mutableListOf(
                                  Container().apply {
                                    name = containerName
                                    env =
                                        envNames
                                            .map { envName -> EnvVar(envName, "v", null) }
                                            .toMutableList()
                                  }
                              )
                        }
                  }
            }
      }
}
