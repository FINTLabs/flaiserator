package no.fintlabs.operator

import io.javaoperatorsdk.operator.api.config.ControllerConfiguration
import io.javaoperatorsdk.operator.api.config.dependent.DependentResourceSpec
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResourceFactory
import kotlin.reflect.KClass
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.Qualifier

class KoinDependentResourceFactory<C : ControllerConfiguration<*>> :
    DependentResourceFactory<C, DependentResourceSpec<*, *, *>>, KoinComponent {
  private val knownDependents = mutableMapOf<Pair<KClass<*>, Qualifier?>, DependentResource<*, *>>()

  override fun createFrom(
      spec: DependentResourceSpec<*, *, *>,
      controllerConfiguration: C,
  ): DependentResource<*, *> {
    if (spec !is KoinDependentResourceSpec<*, *>) {
      error("${spec.javaClass.canonicalName} cannot be instantiated. Not KoinDependentResourceSpec")
    }

    return createFromKoinSpec(spec, controllerConfiguration)
  }

  private fun createFromKoinSpec(
      spec: KoinDependentResourceSpec<*, *>,
      controllerConfiguration: C,
  ): DependentResource<*, *> {
    val dependentResourceKClass = spec.dependentResourceClass.kotlin
    val qualifier = spec.qualifier

    return knownDependents.getOrPut(dependentResourceKClass to qualifier) {
      @Suppress("UNCHECKED_CAST")
      val dependent =
          getKoin().get(clazz = dependentResourceKClass, qualifier = qualifier)
              as DependentResource<*, *>

      dependent.also { configure(it, spec, controllerConfiguration) }
    }
  }
}
