package com.frankie.expressionmanager.dependency

import com.frankie.expressionmanager.model.ReservedCode
import com.frankie.expressionmanager.context.hasStateInstruction
import com.frankie.expressionmanager.ext.isGroupCode
import com.frankie.expressionmanager.ext.isQuestionCode
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.codes

fun List<SurveyComponent>.componentIndices(
    parentIndex: ComponentIndex? = null,
    parentRandomInstruction: Instruction.RandomGroups? = null,
    parentPriority: Instruction.PriorityGroups? = null
): List<ComponentIndex> {
    val returnList = mutableListOf<ComponentIndex>()
    val indices = indices(parentRandomInstruction)
    forEachIndexed { index, surveyComponent ->
        if (surveyComponent.noErrors()) {
            val code = surveyComponent.uniqueCode(parentIndex?.code ?: "")
            val hasUniqueCode = surveyComponent.hasUniqueCode()
            val componentIndex = ComponentIndex(
                code = code,
                parent = parentIndex?.code,
                minIndex = indices.first[index],
                maxIndex = indices.second[index],
                dependencies = surveyComponent.accessibleDependencies().toSet(),
                children = surveyComponent.children.map { it.uniqueCode(code) },
                prioritisedSiblings = parentPriority?.codes()
                    ?.firstOrNull { it.contains(surveyComponent.code) }
                    ?.map {
                        if (hasUniqueCode) it else (parentIndex?.code ?: "") + it
                    }?.toSet() ?: setOf()
            )
            returnList.add(componentIndex)
            val randomInstruction =
                surveyComponent.instructionList.filterIsInstance<Instruction.RandomGroups>().firstOrNull()
            val priorityInstruction =
                surveyComponent.instructionList.filterIsInstance<Instruction.PriorityGroups>().firstOrNull()
            returnList.addAll(
                surveyComponent.children.componentIndices(
                    componentIndex,
                    randomInstruction,
                    priorityInstruction
                )
            )
        }
    }
    return returnList

}

fun List<SurveyComponent>.indices(
    parentRandomInstruction: Instruction.RandomGroups? = null
): Pair<List<Int>, List<Int>> {
    val indexList = mapIndexed { index, surveyComponent -> Pair(surveyComponent.code, index) }.toMutableList()
    val randomCodes = parentRandomInstruction?.codes() ?: setOf()
    val childrenWithActiveOrder =
        filter { it.hasStateInstruction(ReservedCode.Order, true) }.map { it.code }.toSet()
    val allCodes = randomCodes.toMutableSet().apply { add(childrenWithActiveOrder) }


    indexList.forEach { pair ->
        val code = pair.first
        val allRandomSetContainingCode = allCodes.filter { stringSet -> stringSet.any { it == code } }
        if (allRandomSetContainingCode.isNotEmpty()) {
            val involvedChildren =
                indexList.filter { indexItem -> allRandomSetContainingCode.any { it.contains(indexItem.first) } }
            val minIndex = involvedChildren.minByOrNull { it.second }!!.second
            involvedChildren.forEach { involvedChild ->
                val involvedChildCode = involvedChild.first
                val involvedChildIndex = indexList.indexOfFirst { it.first == involvedChildCode }
                indexList[involvedChildIndex] = Pair(involvedChildCode, minIndex)
            }
        }
    }
    val minIndex = indexList.map { it.second }
    val maxIndex = minIndex.map { index -> minIndex.lastIndexOf(index) }
    return Pair(minIndex, maxIndex)
}

fun List<ComponentIndex>.accessibleDependencies(code: String): List<Dependency> {
    val dependencies: MutableList<Dependency> = mutableListOf()
    val componentIndex = first { it.code == code }
    if (componentIndex.parent != null) {
        val parents = parents(componentIndex)
        parents.forEach { parent ->
            dependencies.addAll(accessibleSiblings(parent))
            dependencies.addAll(parent.dependencies.filter { it.accessibleByChildren }
                .map { Dependency(parent.code, it) })
        }
    }
    dependencies.addAll(accessibleSiblings(componentIndex))
    dependencies.addAll(childrenDependencies(componentIndex))
    return dependencies
}

fun List<ComponentIndex>.jumpDestinations(code: String): List<String> {
    val dependencies: MutableList<String> = mutableListOf()
    val componentIndex = first { it.code == code }
    if (componentIndex.parent != null) {
        val parents = parents(componentIndex)
        parents.forEach { parent ->
            if (parent.code.isQuestionCode() || parent.code.isGroupCode()) {
                dependencies.addAll(jumpToSiblings(parent))
            }
        }
    }
    if (componentIndex.code.isQuestionCode() || componentIndex.code.isGroupCode()) {
        dependencies.addAll(jumpToSiblings(componentIndex))
    }
    return dependencies
}

private fun List<ComponentIndex>.parents(componentIndex: ComponentIndex): List<ComponentIndex> {
    val componentIndices: MutableList<ComponentIndex> = mutableListOf()
    val parent = first { it.code == componentIndex.parent }
    if (parent.parent != null) {
        componentIndices.add(parent)
        componentIndices.addAll(parents(parent))
    }
    return componentIndices

}

private fun List<ComponentIndex>.accessibleSiblings(componentIndex: ComponentIndex): List<Dependency> {
    val dependencies: MutableList<Dependency> = mutableListOf()
    val accessibleSiblings =
        filter {
            it.parent == componentIndex.parent
                    && it.maxIndex < componentIndex.minIndex
                    && !componentIndex.prioritisedSiblings.contains(it.code)
        }
    accessibleSiblings.forEach { siblingIndex ->
        dependencies.addAll(siblingIndex.mapDependencies())
        dependencies.addAll(childrenDependencies(siblingIndex))
    }
    return dependencies
}


private fun List<ComponentIndex>.jumpToSiblings(componentIndex: ComponentIndex): List<String> {
    val dependencies: MutableList<String> = mutableListOf()
    val accessibleSiblings =
        filter {
            it.parent == componentIndex.parent
                    && it.minIndex > componentIndex.maxIndex
                    && !componentIndex.prioritisedSiblings.contains(it.code)
        }
    accessibleSiblings.forEach { siblingIndex ->
        dependencies.add(siblingIndex.code)
        dependencies.addAll(childrenDependencies(siblingIndex)
            .map { it.componentCode }
            .distinct()
            .filter { it.isQuestionCode() || it.isGroupCode() })
    }
    return dependencies
}


private fun List<ComponentIndex>.childrenDependencies(componentIndex: ComponentIndex): List<Dependency> {
    val dependencies: MutableList<Dependency> = mutableListOf()
    componentIndex.children.forEach { childCode ->
        val childComponentIndex = first { it.code == childCode }
        dependencies.addAll(childComponentIndex.mapDependencies())
        dependencies.addAll(childrenDependencies(childComponentIndex))
    }
    return dependencies
}

fun ComponentIndex.mapDependencies() = dependencies.map { Dependency(code, it) }