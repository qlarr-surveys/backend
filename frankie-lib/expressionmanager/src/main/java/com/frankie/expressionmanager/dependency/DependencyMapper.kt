package com.frankie.expressionmanager.dependency

import com.frankie.expressionmanager.model.*


internal class DependencyMapper {
    val impactMap: ImpactMap get() = mutableImpactMap
    val dependencyMap: DependencyMap get() = mutableDependencyMap

    private val mutableImpactMap: MutableImpactMap = mutableMapOf()
    private val mutableDependencyMap: MutableDependencyMap = mutableMapOf()

    constructor(stringImpactMap: StringImpactMap) {
        mutableImpactMap.putAll(stringImpactMap.toImpactMap())
        mutableDependencyMap.putAll(impactMap.toDependencyMap())

    }

    constructor(qualifiedComponents: List<ChildlessComponent> = mutableListOf()) {
        mutableDependencyMap.putAll(resolveDependencyMap(qualifiedComponents))
        mutableImpactMap.putAll(dependencyMap.toImpactMap())
    }

    private fun resolveDependencyMap(qualifiedComponents: List<ChildlessComponent>): DependencyMap {
        val dependentsMap = hashMapOf<Dependent, List<Dependency>>()
        qualifiedComponents.forEach { component ->
            component
                .instructionList
                .filter { it is Instruction.State && it.isActive || it is Instruction.Reference }
                .forEach { instruction ->
                    val text = (instruction as? Instruction.State)?.text
                        ?: (instruction as Instruction.Reference).text()
                    val dependencyList = getDependencyList(qualifiedComponents, text)
                    if (dependencyList.isNotEmpty()) {
                        dependentsMap[Dependent(component.code, instruction.code)] = dependencyList
                    }
                }
        }
        return dependentsMap

    }

    private fun getDependencyList(
        qualifiedComponents: List<ChildlessComponent>,
        instructionText: String
    ): List<Dependency> {
        val impactedList = mutableListOf<Dependency>()
        qualifiedComponents.forEach { component ->
            if (instructionText.contains(component.code)) {
                impactedList.addAll(component.getDependencies().filter { instructionText.contains(it.asCode()) })
            }
        }
        return impactedList
    }
}

fun ImpactMap.toDependencyMap(): DependencyMap {
    val dependentsMap: MutableDependencyMap = hashMapOf()
    forEach { entry ->
        dependentsMap.insertDependency(entry.key, entry.value)
    }
    return dependentsMap
}

fun DependencyMap.toImpactMap(): ImpactMap {
    val impactMap: MutableImpactMap = hashMapOf()
    forEach { entry ->
        impactMap.insertDependent(entry.key, entry.value)
    }
    return impactMap
}

private fun MutableDependencyMap.insertDependency(
    dependency: Dependency,
    dependents: List<Dependent>
) {
    dependents.forEach { dependent ->
        if (keys.contains(dependent)) {
            val list = get(dependent)!!
            if (!list.contains(dependency)) {
                put(dependent, list.toMutableList().apply { add(dependency) })
            }
        } else {
            put(dependent, listOf(dependency))
        }
    }
}

private fun MutableImpactMap.insertDependent(
    dependent: Dependent,
    dependencies: List<Dependency>
) {
    dependencies.forEach { dependency ->
        if (keys.contains(dependency)) {
            val list = get(dependency)!!
            if (!list.contains(dependent)) {
                put(dependency, list.toMutableList().apply { add(dependent) })
            }
        } else {
            put(dependency, listOf(dependent))
        }
    }
}


fun Dependency.toDependent() = Dependent(componentCode, reservedCode.code)
