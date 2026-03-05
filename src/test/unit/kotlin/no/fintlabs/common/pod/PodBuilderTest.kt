package no.fintlabs.common.pod

import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.LocalObjectReference
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import no.fintlabs.Config
import no.fintlabs.application.api.ORG_ID_LABEL
import no.fintlabs.common.TestResource
import no.fintlabs.common.TestSpec

class PodBuilderTest {
  @Test
  fun `should normalize env and keep last duplicate value`() {
    val primary =
        createPrimaryResource(
            spec =
                TestSpec(
                    env =
                        listOf(
                            EnvVar("key", "first", null),
                            EnvVar("fint.org-id", "override-org", null),
                            EnvVar("key", "second", null),
                        )
                )
        )
    val podTemplate =
        createPodBuilder().build(
            primary,
            mockk<Context<TestResource>>(relaxed = true),
            {
              ObjectMeta().apply {
                annotations = mutableMapOf()
                labels = mutableMapOf()
              }
            },
        ) { builderContext ->
          builderContext.containers +=
              Container().apply {
                name = primary.metadata.name
                env = builderContext.getNormalizedEnv()
              }
        }

    val appContainer = podTemplate.spec.containers.first()
    assertEquals("override-org", appContainer.env.find { it.name == "fint.org-id" }?.value)
    assertEquals("second", appContainer.env.find { it.name == "key" }?.value)
    assertEquals(3, appContainer.env.size)
  }

  @Test
  fun `should convert empty env values to null`() {
    val primary =
        createPrimaryResource(
            spec =
                TestSpec(
                    env =
                        listOf(
                            EnvVar("empty", "", null),
                            EnvVar("set", "value", null),
                        )
                )
        )
    val podTemplate =
        createPodBuilder().build(
            primary,
            mockk<Context<TestResource>>(relaxed = true),
            {
              ObjectMeta().apply {
                annotations = mutableMapOf()
                labels = mutableMapOf()
              }
            },
        ) { builderContext ->
          builderContext.containers +=
              Container().apply {
                name = primary.metadata.name
                env = builderContext.getNormalizedEnv()
              }
        }

    val appContainer = podTemplate.spec.containers.first()
    val empty = appContainer.env.find { it.name == "empty" }
    assertNotNull(empty)
    assertEquals(null, empty.value)
  }

  @Test
  fun `should merge image pull secrets and move app container first`() {
    val primary =
        createPrimaryResource(
            spec = TestSpec(imagePullSecrets = listOf("app", "shared")),
        )
    val podBuilder =
        createPodBuilder(config = Config(imagePullSecrets = listOf("shared", "global")))
    val podTemplate =
        podBuilder.build(
            primary,
            mockk<Context<TestResource>>(relaxed = true),
            {
              ObjectMeta().apply {
                annotations = mutableMapOf()
                labels = mutableMapOf()
              }
            },
        ) { builderContext ->
          builderContext.containers += Container().apply { name = "sidecar" }
          builderContext.containers += Container().apply { name = primary.metadata.name }
        }

    assertEquals(primary.metadata.name, podTemplate.spec.containers.first().name)
    assertEquals(
        setOf("app", "shared", "global"),
        podTemplate.spec.imagePullSecrets.map(LocalObjectReference::getName).toSet(),
    )
  }

  @Test
  fun `should apply customizers to metadata and pod spec`() {
    val primary = createPrimaryResource(spec = TestSpec())
    val podBuilder =
        createPodBuilder(
            customizers =
                arrayOf(
                    object : PodCustomizer<TestResource> {
                      override fun customizePod(
                          primary: TestResource,
                          builderContext: PodBuilderContext,
                          context: Context<TestResource>,
                      ) {
                        builderContext.annotations["custom-annotation"] = "enabled"
                        builderContext.labels["custom-label"] = "yes"
                        builderContext.containers += Container().apply { name = "sidecar" }
                      }
                    }
                )
        )

    val podTemplate =
        podBuilder.build(
            primary,
            mockk<Context<TestResource>>(relaxed = true),
            {
              ObjectMeta().apply {
                annotations = mutableMapOf()
                labels = mutableMapOf()
              }
            },
        ) { builderContext ->
          builderContext.containers += Container().apply { name = primary.metadata.name }
        }

    assertEquals("enabled", podTemplate.metadata.annotations["custom-annotation"])
    assertEquals("yes", podTemplate.metadata.labels["custom-label"])
    assertEquals(primary.metadata.name, podTemplate.spec.containers.first().name)
    assertEquals("sidecar", podTemplate.spec.containers[1].name)
  }

  @Test
  fun `should apply customizers before pod spec configuration`() {
    val primary = createPrimaryResource(spec = TestSpec())
    val podBuilder =
        createPodBuilder(
            customizers =
                arrayOf(
                    object : PodCustomizer<TestResource> {
                      override fun customizePod(
                          primary: TestResource,
                          builderContext: PodBuilderContext,
                          context: Context<TestResource>,
                      ) {
                        builderContext.env += EnvVar("custom-key", "from-customizer", null)
                      }
                    }
                )
        )

    val podTemplate =
        podBuilder.build(
            primary,
            mockk<Context<TestResource>>(relaxed = true),
            {
              ObjectMeta().apply {
                annotations = mutableMapOf()
                labels = mutableMapOf()
              }
            },
        ) { builderContext ->
          builderContext.containers +=
              Container().apply {
                name = primary.metadata.name
                env = builderContext.getNormalizedEnv()
              }
        }

    val appContainer = podTemplate.spec.containers.first()
    assertEquals("from-customizer", appContainer.env.find { it.name == "custom-key" }?.value)
  }

  @Test
  fun `should fail when app container is missing`() {
    val primary = createPrimaryResource(spec = TestSpec())

    val error =
        assertFailsWith<IllegalStateException> {
          createPodBuilder().build(
              primary,
              mockk<Context<TestResource>>(relaxed = true),
              {
                ObjectMeta().apply {
                  annotations = mutableMapOf()
                  labels = mutableMapOf()
                }
              },
          ) { builderContext ->
            builderContext.containers += Container().apply { name = "sidecar" }
          }
        }

    assertEquals("App container 'test-app' not found in Pod configuration", error.message)
  }

  private fun createPodBuilder(
      config: Config = Config(),
      customizers: Array<PodCustomizer<TestResource>> = emptyArray(),
  ) = PodBuilder.create(config, *customizers)

  private fun createPrimaryResource(spec: TestSpec): TestResource =
      TestResource().apply {
        metadata =
            ObjectMeta().apply {
              name = "test-app"
              labels = mutableMapOf(ORG_ID_LABEL to "test.org")
            }
        this.spec = spec
      }
}
