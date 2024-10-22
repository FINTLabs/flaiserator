package no.fintlabs.operator.matcher

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.ReplicaSet
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import io.javaoperatorsdk.operator.OperatorException
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.LoggingUtils
import no.fintlabs.operator.getLogger
import java.util.*

class CustomGenericKubernetesResourceMatcher<R : HasMetadata> {

    companion object {
        private val INSTANCE = CustomGenericKubernetesResourceMatcher<HasMetadata>()
        const val APPLY_OPERATION = "Apply"
        const val DOT_KEY = "."

        private val IGNORED_METADATA = listOf("creationTimestamp", "deletionTimestamp", "generation", "selfLink", "uid")

        @Suppress("UNCHECKED_CAST")
        fun <L : HasMetadata> getInstance(): CustomGenericKubernetesResourceMatcher<L> = INSTANCE as CustomGenericKubernetesResourceMatcher<L>

        private const val F_PREFIX = "f:"
        private const val K_PREFIX = "k:"
        private const val V_PREFIX = "v:"
        private const val METADATA_KEY = "metadata"
        private const val NAME_KEY = "name"
        private const val NAMESPACE_KEY = "namespace"
        private const val KIND_KEY = "kind"
        private const val API_VERSION_KEY = "apiVersion"

        private val log = getLogger()
    }

    @Suppress("UNCHECKED_CAST")
    fun matches(actual: R, desired: R, context: Context<*>): Boolean {
        val optionalManagedFieldsEntry = checkIfFieldManagerExists(actual, context.controllerConfiguration.fieldManager())
        if (optionalManagedFieldsEntry.isEmpty) {
            return false
        }

        val managedFieldsEntry = optionalManagedFieldsEntry.get()

        val objectMapper = context.client.kubernetesSerialization
        val actualMap = objectMapper.convertValue(actual, MutableMap::class.java) as MutableMap<String, Any>
        val desiredMap = objectMapper.convertValue(desired, MutableMap::class.java) as MutableMap<String, Any>

        sanitizeState(actual, desired, actualMap)

        normalizeValuesForComparison(desired, desiredMap)
        normalizeValuesForComparison(actual, actualMap)

        if (LoggingUtils.isNotSensitiveResource(desired)) {
            log.trace("Original actual: \n {} \n original desired: \n {} ", actual, desiredMap)
        }

        val prunedActual = HashMap<String, Any>(actualMap.size)
        keepOnlyManagedFields(prunedActual, actualMap, managedFieldsEntry.fieldsV1.additionalProperties, objectMapper)

        removeIrrelevantValues(desiredMap)

        if (LoggingUtils.isNotSensitiveResource(desired)) {
            log.debug("Pruned actual: \n {} \n desired: \n {} ", prunedActual, desiredMap)
        }
        return prunedActual == desiredMap
    }

    private fun sanitizeState(actual: R, desired: R, actualMap: MutableMap<String, Any>) {
        if (desired is StatefulSet && actual is StatefulSet) {
            val desiredStatefulSet = desired as StatefulSet
            val actualStatefulSet = actual as StatefulSet
            val claims = desiredStatefulSet.spec.volumeClaimTemplates.size
            if (claims == actualStatefulSet.spec.volumeClaimTemplates.size) {
                for (i in 0 until claims) {
                    if (desiredStatefulSet.spec.volumeClaimTemplates[i].spec?.volumeMode == null) {
                        GenericKubernetesResource.get<MutableMap<String, Any>?>(actualMap, "spec", "volumeClaimTemplates", i, "spec")?.remove("volumeMode")
                    }
                    if (desiredStatefulSet.spec.volumeClaimTemplates[i].status == null) {
                        GenericKubernetesResource.get<MutableMap<String, Any>?>(actualMap, "spec", "volumeClaimTemplates", i)?.remove("status")
                    }
                }
            }
        }
    }

    private fun normalizeValuesForComparison(resource: R, resourceMap: MutableMap<String, Any>) {
        when (resource) {
            is Pod -> resource.spec.containers.normalizeContainers(resourceMap)
            is Deployment -> resource.spec.template.spec.containers.normalizeContainers(resourceMap)
            is Job -> resource.spec.template.spec.containers.normalizeContainers(resourceMap)
            is ReplicaSet -> resource.spec.template.spec.containers.normalizeContainers(resourceMap)
            is StatefulSet -> {
                resource.spec.template.spec.containers.normalizeContainers(resourceMap)
                resource.spec.volumeClaimTemplates.forEachIndexed { i, pvc ->
                    pvc.spec.resources?.normalizeResources(resourceMap, "spec", "volumeClaimTemplates", i, "spec", "resources")
                }
            }
            is PersistentVolumeClaim -> resource.spec.resources?.normalizeResources(resourceMap, "spec", "resources")
            is PersistentVolume -> resource.spec.capacity?.normalizeQuantity(resourceMap, "spec", "capacity")
        }
    }

    private fun List<Container>.normalizeContainers(map: MutableMap<String, Any>) {
        this.forEachIndexed { i, container ->
            container.resources?.normalizeResources(map, "spec", "template", "spec", "containers", i, "resources")
        }
    }

    private fun ResourceRequirements.normalizeResources(map: MutableMap<String, Any>, vararg path: Any) {
        this.limits?.normalizeQuantity(map, *path, "limits")
        this.requests?.normalizeQuantity(map, *path, "requests")
    }

    private fun VolumeResourceRequirements.normalizeResources(map: MutableMap<String, Any>, vararg path: Any) {
        this.limits?.normalizeQuantity(map, *path, "limits")
        this.requests?.normalizeQuantity(map, *path, "requests")
    }

    private fun Map<String, Quantity>.normalizeQuantity(map: MutableMap<String, Any>, vararg path: Any) {
        GenericKubernetesResource.get<MutableMap<String, Any>?>(map, *path)?.let { resourceMap ->
            this.forEach { (key, value) ->
                resourceMap[key] = value.numericalAmount.stripTrailingZeros()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun removeIrrelevantValues(desiredMap: MutableMap<String, Any>) {
        val metadata = desiredMap[METADATA_KEY] as MutableMap<String, Any>
        metadata.remove(NAME_KEY)
        metadata.remove(NAMESPACE_KEY)
        IGNORED_METADATA.forEach { metadata.remove(it) }
        if (metadata.isEmpty()) {
            desiredMap.remove(METADATA_KEY)
        }
        desiredMap.remove(KIND_KEY)
        desiredMap.remove(API_VERSION_KEY)
    }

    @Suppress("UNCHECKED_CAST")
    private fun keepOnlyManagedFields(
        result: MutableMap<String, Any>,
        actualMap: Map<String, Any>,
        managedFields: Map<String, Any>,
        objectMapper: KubernetesSerialization
    ) {
        if (managedFields.isEmpty()) {
            result.putAll(actualMap)
            return
        }
        for ((key, value) in managedFields) {
            if (key.startsWith(F_PREFIX)) {
                val keyInActual = keyWithoutPrefix(key)
                val managedFieldValue = value as Map<String, Any>
                if (isNestedValue(managedFieldValue)) {
                    val managedEntrySet = managedFieldValue.entries

                    if (isListKeyEntrySet(managedEntrySet)) {
                        handleListKeyEntrySet(result, actualMap, objectMapper, keyInActual, managedEntrySet)
                    } else if (isSetValueField(managedEntrySet)) {
                        handleSetValues(result, actualMap, objectMapper, keyInActual, managedEntrySet)
                    } else {
                        fillResultsAndTraverseFurther(result, actualMap, managedFields, objectMapper, key, keyInActual, managedFieldValue)
                    }
                } else {
                    result[keyInActual] = actualMap[keyInActual] ?: ""
                }
            } else if (key != DOT_KEY) {
                throw IllegalStateException("Key: $key has no prefix: $F_PREFIX")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fillResultsAndTraverseFurther(
        result: MutableMap<String, Any>,
        actualMap: Map<String, Any>,
        managedFields: Map<String, Any>,
        objectMapper: KubernetesSerialization,
        key: String,
        keyInActual: String,
        managedFieldValue: Any
    ) {
        val emptyMapValue = HashMap<String, Any>()
        result[keyInActual] = emptyMapValue
        val actualMapValue = actualMap.getOrDefault(keyInActual, emptyMap<String, Any>()) as Map<String, Any>

        keepOnlyManagedFields(emptyMapValue, actualMapValue, managedFields[key] as Map<String, Any>, objectMapper)
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleListKeyEntrySet(
        result: MutableMap<String, Any>,
        actualMap: Map<String, Any>,
        objectMapper: KubernetesSerialization,
        keyInActual: String,
        managedEntrySet: Set<Map.Entry<String, Any>>
    ) {
        val valueList = mutableListOf<Any>()
        result[keyInActual] = valueList
        val actualValueList = actualMap[keyInActual] as List<Map<String, Any>>

        val targetValuesByIndex = TreeMap<Int, Map<String, Any>>()
        val managedEntryByIndex = mutableMapOf<Int, Map<String, Any>>()

        for ((listEntryKey, listEntryValue) in managedEntrySet) {
            if (listEntryKey == DOT_KEY) continue
            val actualListEntry = selectListEntryBasedOnKey(keyWithoutPrefix(listEntryKey), actualValueList, objectMapper)
            targetValuesByIndex[actualListEntry.key] = actualListEntry.value
            managedEntryByIndex[actualListEntry.key] = listEntryValue as Map<String, Any>
        }

        targetValuesByIndex.forEach { (key, value) ->
            val emptyResMapValue = mutableMapOf<String, Any>()
            valueList.add(emptyResMapValue)
            keepOnlyManagedFields(emptyResMapValue, value, managedEntryByIndex[key]!!, objectMapper)
        }
    }

    private fun handleSetValues(
        result: MutableMap<String, Any>,
        actualMap: Map<String, Any>,
        objectMapper: KubernetesSerialization,
        keyInActual: String,
        managedEntrySet: Set<Map.Entry<String, Any>>
    ) {
        val valueList = mutableListOf<Any>()
        result[keyInActual] = valueList

        val values = actualMap[keyInActual] as List<*>
        val targetClass = values[0]?.javaClass

        for ((valueEntryKey, _) in managedEntrySet) {
            if (valueEntryKey == DOT_KEY) continue

            val value = parseKeyValue(keyWithoutPrefix(valueEntryKey), targetClass, objectMapper)
            valueList.add(value)
        }
    }

    fun parseKeyValue(stringValue: String, targetClass: Class<*>?, objectMapper: KubernetesSerialization): Any {
        val trimmedValue = stringValue.trim()
        return targetClass?.let {
            objectMapper.unmarshal(trimmedValue, it)
        } ?: objectMapper.unmarshal(trimmedValue, Map::class.java)
    }

    private fun checkIfFieldManagerExists(actual: R, fieldManager: String): Optional<ManagedFieldsEntry> {
        val targetManagedFields = actual.metadata.managedFields?.filter {
            it.manager == fieldManager && it.operation == APPLY_OPERATION
        }.orEmpty()

        if (targetManagedFields.isEmpty()) {
            log.debug("No field manager exists for resource ${actual.kind} with name: ${actual.metadata.name} and operation Apply ")
            return Optional.empty()
        }

        if (targetManagedFields.size > 1) {
            throw OperatorException("More than one field manager exists with name: $fieldManager in resource: ${actual.kind} with name: ${actual.metadata.name}")
        }

        return Optional.of(targetManagedFields[0])
    }

    private fun keyWithoutPrefix(key: String): String = key.substring(2)

    private fun isNestedValue(managedFieldValue: Map<*, *>): Boolean = managedFieldValue.isNotEmpty()

    private fun isSetValueField(managedEntrySet: Set<Map.Entry<String, Any>>): Boolean = isKeyPrefixedSkippingDotKey(managedEntrySet, V_PREFIX)

    private fun isListKeyEntrySet(managedEntrySet: Set<Map.Entry<String, Any>>): Boolean = isKeyPrefixedSkippingDotKey(managedEntrySet, K_PREFIX)

    private fun isKeyPrefixedSkippingDotKey(managedEntrySet: Set<Map.Entry<String, Any>>, prefix: String): Boolean {
        val iterator = managedEntrySet.iterator()
        var managedFieldEntry = iterator.next()
        if (managedFieldEntry.key == DOT_KEY) {
            managedFieldEntry = iterator.next()
        }
        return managedFieldEntry.key.startsWith(prefix)
    }

    @Suppress("UNCHECKED_CAST")
    private fun selectListEntryBasedOnKey(key: String, values: List<Map<String, Any>>, objectMapper: KubernetesSerialization): Map.Entry<Int, Map<String, Any>> {
        val ids = objectMapper.unmarshal(key, Map::class.java) as Map<String, Any>
        val possibleTargets = values.filter { it.entries.containsAll(ids.entries) }
        val index = values.indexOfFirst { it.entries.containsAll(ids.entries) }

        if (possibleTargets.isEmpty()) {
            throw IllegalStateException("Cannot find list element for key: $key, in map: ${values.map { it.keys }}")
        }
        if (possibleTargets.size > 1) {
            throw IllegalStateException("More targets found in list element for key: $key, in map: ${values.map { it.keys }}")
        }

        return AbstractMap.SimpleEntry(index, possibleTargets[0])
    }
}
