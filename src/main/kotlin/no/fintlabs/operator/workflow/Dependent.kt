package no.fintlabs.operator.workflow

import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource
import kotlin.reflect.KClass
import org.koin.core.qualifier.QualifierValue

annotation class Dependent(
    val dependentClass: KClass<out DependentResource<*, *>>,
    val qualifier: QualifierValue = "",
    val dependsOn: Array<DependentRef> = [],
)
