package com.frankie.expressionmanager.context.execute

import com.frankie.expressionmanager.model.ReservedCode.*
import com.frankie.expressionmanager.context.build.NotSkippedInstructionManifesto
import com.frankie.expressionmanager.dependency.toDependent
import com.frankie.expressionmanager.ext.VALID_QUESTION_CODE
import com.frankie.expressionmanager.model.Dependency
import com.frankie.expressionmanager.model.DependencyMap
import com.frankie.expressionmanager.model.Instruction.State

internal class ContextRunner(
    private val instructionsMap: LinkedHashMap<Dependency, State>,
    private val dependencyMap: DependencyMap,
    private val orderPriorityValues: Map<Dependency, Int>,
    private val skipMap: Map<String, List<NotSkippedInstructionManifesto>>
) {

    fun navigationDependencies(): Set<Dependency> {
        return instructionsMap.keys.filter { dependency ->
            when {
                dependency.componentCode == "Survey" -> {
                    dependency.reservedCode in listOf(Validity)
                }
                dependency.componentCode.matches(Regex(VALID_QUESTION_CODE)) -> {
                    dependency.reservedCode in listOf(Relevance, Validity)
                }
                dependency.componentCode.startsWith("G") -> {
                    dependency.reservedCode in listOf(Relevance, Validity)
                }
                else -> {
                    false
                }
            }
        }.toHashSet()
    }


    fun instructionsRefreshSequence(): List<Dependency> {
        val inactiveInstructions = instructionsMap.filterValues { !it.isActive }.keys.toList()
        val activeInstructions = instructionsMap.filterValues { it.isActive }.keys.toList()
        return inactiveInstructions.toMutableList()
            .apply {
                addAll(
                    dependencyMap
                        .filterCyclic()
                        .getSequence(activeInstructions)
                )
            }
    }

    private fun DependencyMap.filterCyclic(): DependencyMap {
        val returnMap = dependencyMap.toMutableMap()
        returnMap.keys.forEach { dependent ->
            // we attempt to break the priority chain...
            // More prioritised components don't need to worry about less prioritised ones
            if (dependent.instructionCode == Prioritised.code) {
                get(dependent)?.let { dependencies ->
                    val priority = orderPriorityValues[Dependency(dependent.componentCode, Priority)]!!
                    val otherPriorities =
                        dependencies.map { Dependency(it.componentCode, Priority) }.map { orderPriorityValues[it] }
                    returnMap[dependent] =
                        dependencies.filterIndexed { index, dep -> dep.componentCode == dependent.componentCode || otherPriorities[index]!! < priority }
                }
            }
            // Skip instructions within random components will also cause cyclic chain...
            // Components whose order is resolved and when placed outside the from/to components
            // don't need to worry about skip instructions
            else if (dependent.instructionCode == NotSkipped.code && skipMap[dependent.componentCode]?.any { it.anyOrder() } == true) {
                get(dependent)?.let { dependencies ->
                    val returnDependencies = dependencies.toMutableList()
                    val order = orderPriorityValues[Dependency(dependent.componentCode, Order)]!!
                    skipMap[dependent.componentCode]?.filter { it.anyOrder() }?.forEach { skipManifesto ->
                        if (skipManifesto.fromOrderNecessary && order < orderPriorityValues[Dependency(
                                skipManifesto.fromComponent,
                                Order
                            )]!!
                        ) {
                            returnDependencies.remove(skipManifesto.dependency)
                        }
                        if (skipManifesto.toOrderNecessary && order > orderPriorityValues[Dependency(
                                skipManifesto.toComponent,
                                Order
                            )]!!
                        ) {
                            returnDependencies.remove(skipManifesto.dependency)
                        }
                    }
                    returnMap[dependent] = returnDependencies
                }
            }
        }
        return returnMap
    }


}

// only visible for Testing purposes
fun DependencyMap.getSequence(
    toBeSorted: List<Dependency>
): List<Dependency> {
    val returnList = toBeSorted.toMutableList()
    var sortingHappened = false
    toBeSorted.forEachIndexed { index, dependency ->
        val dependencies: List<Dependency>? = get(dependency.toDependent())
        if (dependencies?.isNotEmpty() == true) {
            val potentialIndex: Int = dependencies.maxOf { toBeSorted.indexOf(it) }
            if (index < potentialIndex) {
                sortingHappened = true
                returnList.remove(dependency)
                if (potentialIndex > returnList.size - 1) {
                    returnList.add(dependency)
                } else {
                    returnList.add(potentialIndex, dependency)
                }
            }
        }
    }
    return if (sortingHappened) {
        getSequence(returnList)
    } else {
        returnList
    }
}
