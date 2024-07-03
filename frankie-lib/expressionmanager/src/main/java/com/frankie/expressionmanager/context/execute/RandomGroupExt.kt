package com.frankie.expressionmanager.context.execute

import com.frankie.expressionmanager.ext.isUniqueCode
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.*
import com.frankie.expressionmanager.model.Instruction.RandomOption.*
import com.frankie.expressionmanager.model.ReservedCode.Order
import com.frankie.expressionmanager.model.ReservedCode.Priority
import kotlin.math.absoluteValue
import kotlin.random.Random


@Suppress("UNCHECKED_CAST")
internal fun Survey.randomize(randomOptions: List<RandomOption>, getLabel: (String) -> String): Map<Dependency, Int> {
    return (mutableListOf(this) as MutableList<SurveyComponent>).randomizeChildren(
        getLabel = getLabel,
        randomOptions = randomOptions
    )
}

@Suppress("UNCHECKED_CAST")
internal fun Survey.setPriorities(): Map<Dependency, Int> {
    return (mutableListOf(this) as MutableList<SurveyComponent>).setChildrenPriorities()
}

internal fun List<SurveyComponent>.randomizeChildren(
    parentCode: String = "",
    pendulumDirection: FlipDirection = FlipDirection.values()[Random.nextInt().absoluteValue % 2],
    getLabel: (String) -> String = { "" },
    randomOptions: List<RandomOption> = RandomOption.values().toList()
): Map<Dependency, Int> {
    val mutableMap = mutableMapOf<Dependency, Int>()
    forEach { surveyComponent ->
        if (surveyComponent.noErrors()) {
            val code = surveyComponent.uniqueCode(parentCode)
            surveyComponent.randomGroups(randomOptions).forEach {
                mutableMap.putAll(surveyComponent.children.randomizeChildren(code, it, pendulumDirection, getLabel))
            }
            mutableMap.putAll(surveyComponent.children.randomizeChildren(code, pendulumDirection, getLabel, randomOptions))
        }
    }
    return mutableMap
}

internal fun List<SurveyComponent>.setChildrenPriorities(parentCode: String = ""): Map<Dependency, Int> {
    val mutableMap = mutableMapOf<Dependency, Int>()
    forEach { surveyComponent ->
        if (surveyComponent.noErrors()) {
            val code = surveyComponent.uniqueCode(parentCode)
            surveyComponent.priorityGroups().forEach {
                mutableMap.putAll(surveyComponent.children.prioritiseChildren(code, it))
            }
            mutableMap.putAll(surveyComponent.children.setChildrenPriorities(code))
        }
    }
    return mutableMap
}


private fun List<SurveyComponent>.randomizeChildren(
    parentCode: String,
    randomGroup: RandomGroup,
    pendulumDirection: FlipDirection,
    getLabel: (String) -> String
): Map<Dependency, Int> {
    val mutableMap = mutableMapOf<Dependency, Int>()
    val indices = randomGroup.codes.map { item ->
        indexOfFirst { surveyComponent ->
            surveyComponent.code == item
        }
    }
    val orders = randomGroup.codes.map { item ->
        indexOfFirst { it.code == item } + 1
    }.toMutableList().apply {
        if (randomGroup.randomOption == RANDOM) {
            shuffle()
        } else if (randomGroup.randomOption == FLIP && pendulumDirection == FlipDirection.DESCENDING) {
            reverse()
        } else if (randomGroup.randomOption == ALPHA) {
            sortBy {
                val code = randomGroup.codes[it - 1]
                val qualifiedCode = if (code.isUniqueCode()) code else parentCode + code
                getLabel(qualifiedCode)
            }
        }
    }

    indices.forEachIndexed { index, i ->
        val order = orders[index]
        val component = get(i)
        val code = component.uniqueCode(parentCode)
        mutableMap[Dependency(code, Order)] = order
    }
    return mutableMap
}

private fun List<SurveyComponent>.prioritiseChildren(
    parentCode: String,
    priorityGroup: PriorityGroup
): Map<Dependency, Int> {
    val mutableMap = mutableMapOf<Dependency, Int>()
    val weights: List<Float> = priorityGroup.weights.map {
        val random = (1..priorityGroup.limit * 100)
        it.weight * random.random()
    }
    priorityGroup.weights
        .map { Pair(it.code, priorityGroup.weights.indexOf(it)) }
        .sortedBy { weights[it.second] }
        .forEachIndexed { index, pair ->
            val component = first { it.code == pair.first }
            val code = component.uniqueCode(parentCode)
            mutableMap[Dependency(code, Priority)] = index + 1
        }
    return mutableMap
}