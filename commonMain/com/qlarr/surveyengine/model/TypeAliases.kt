package com.qlarr.surveyengine.model

import com.qlarr.surveyengine.model.exposed.ResponseField
import kotlinx.serialization.json.JsonElement

typealias ImpactMap = Map<Dependency, List<Dependent>>
typealias StringImpactMap = Map<String, List<String>>
typealias DependencyMapBundle = Pair<ImpactMap, DependencyMap>
typealias DependencyMap = Map<Dependent, List<Dependency>>
typealias MutableImpactMap = MutableMap<Dependency, List<Dependent>>
typealias MutableDependencyMap = MutableMap<Dependent, List<Dependency>>


fun ImpactMap.toStringImpactMap(): StringImpactMap {
    val returnMap = mutableMapOf<String, List<String>>()
    keys.forEach { dependency ->
        returnMap[dependency.toValueKey()] = get(dependency)!!.map { it.toValueKey() }
    }
    return returnMap
}

fun StringImpactMap.toImpactMap(): ImpactMap {
    val returnMap = mutableMapOf<Dependency, List<Dependent>>()
    keys.forEach { key ->
        key.toDependency()?.let { dependency ->
            returnMap[dependency] = get(key)!!.map { it.toDependent() }
        }
    }
    return returnMap
}

fun Map<String, JsonElement>.withDependencyKeys(schema: List<ResponseField> = listOf()): Map<Dependency, JsonElement> {
    val dependencies = schema.map { it.toDependent() }
    return filterKeys { dependencies.contains(it.toDependent()) }.mapKeys { it.key.toDependency()!! }
}

fun Map<Dependent, JsonElement>.withStringKeys(): Map<String, JsonElement> = mapKeys { it.key.toValueKey() }