package com.frankie.expressionmanager.navigate

import com.frankie.expressionmanager.context.indexableCodes
import com.frankie.expressionmanager.ext.flatten
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.ReservedCode.*


fun Survey.navigate(
    navigationInfo: NavigationInfo,
    navigationMode: NavigationMode,
    navigationBindings: Map<Dependency, Any>,
    skipInvalid: Boolean = false,
    currentIndexValid: Boolean = true,
): NavigationIndex {
    val navigationIndex = when (navigationInfo.navigationDirection) {
        is NavigationDirection.Resume, is NavigationDirection.ChangeLange -> navigationInfo.navigationIndex!!
        NavigationDirection.Start -> firstRelevant(navigationMode, navigationBindings)
        is NavigationDirection.Next -> nextRelevant(navigationInfo, navigationMode, navigationBindings)
        is NavigationDirection.Jump -> navigationInfo.navigationDirection.navigationIndex
        NavigationDirection.Previous -> prevRelevant(navigationInfo, navigationMode, navigationBindings)
    }
    // we can get the whole survey validity from the bindings...
    val surveyValid = navigationBindings[Dependency("Survey", Validity)] as Boolean
    val isSubmitting = navigationIndex is NavigationIndex.End
    val isNextOrJump = navigationInfo.navigationDirection is NavigationDirection.Next
    val shouldNotSkipValid = !skipInvalid && isNextOrJump

    return when {
        (isSubmitting || shouldNotSkipValid) && !currentIndexValid -> {
            navigationInfo.navigationIndex!!.with(true)
        }

        isSubmitting && !surveyValid -> firstInvalid(navigationMode, navigationBindings).with(true)
        else -> navigationIndex.with(false)
    }
}


fun Survey.componentsInCurrentNav(navigationIndex: NavigationIndex): List<String> {
    return when (navigationIndex) {
        is NavigationIndex.Question -> {
            val group = groups.first { group -> group.questions.any { it.code == navigationIndex.questionId } }
            return mutableListOf(group.code).apply {
                add(navigationIndex.questionId)
            }
        }

        is NavigationIndex.Groups -> {
            val returnList = mutableListOf<String>()
            val currentGroups = groups.filter { it.code in navigationIndex.groupIds }
            currentGroups.forEach { group ->
                returnList.add(group.code)
                returnList.addAll(group.questions.map { it.code })
            }
            returnList
        }

        is NavigationIndex.Group -> {
            val group = groups.first { it.code == navigationIndex.groupId }
            return mutableListOf(group.code).apply {
                addAll(group.questions.map { it.code })
            }
        }

        is NavigationIndex.End -> {
            val group = groups.first { it.code == navigationIndex.groupId }
            return mutableListOf(group.code).apply {
                addAll(group.questions.map { it.code })
            }
        }
    }
}


fun Survey.allNavigationCodes(): List<String> {
    val returnList = mutableListOf<String>()
    groups.forEach { group ->
        returnList.add(group.code)
        returnList.addAll(group.questions.map { it.code })
    }
    return returnList
}

fun Survey.navBefore(navigationIndex: NavigationIndex, bindings: Map<Dependency, Any>): List<String> {
    val indexableCodes = children.indexableCodesRemoveDeprioritised(bindings)
    return when (navigationIndex) {
        is NavigationIndex.Groups -> {
            val minIndex = navigationIndex.groupIds.minOfOrNull { indexableCodes.indexOf(it) }!!
            indexableCodes.filterIndexed { index, _ -> index < minIndex }
        }

        is NavigationIndex.Group -> {
            val groupIndex = indexableCodes.indexOf(navigationIndex.groupId)
            indexableCodes.filterIndexed { index, _ -> index < groupIndex }
        }

        is NavigationIndex.Question -> {
            val questionIndex = indexableCodes.indexOf(navigationIndex.questionId)
            val groupIndex =
                indexableCodes.indexOfLast { code -> code.startsWith("G") && indexableCodes.indexOf(code) < questionIndex }
            indexableCodes.filterIndexed { index, code -> index < groupIndex || (code.startsWith("Q") && index < questionIndex) }
        }

        is NavigationIndex.End -> emptyList()
    }
}

fun Survey.navAfter(navigationIndex: NavigationIndex, bindings: Map<Dependency, Any>): List<String> {
    val indexableCodes = children.indexableCodesRemoveDeprioritised(bindings)
    return when (navigationIndex) {
        is NavigationIndex.Groups -> {
            val lastGroupIndex = navigationIndex.groupIds.maxOfOrNull { indexableCodes.indexOf(it) }!!
            val nextGroupIndex =
                indexableCodes.indexOfFirst { code -> code.startsWith("G") && indexableCodes.indexOf(code) > lastGroupIndex }
            if (nextGroupIndex == -1) {
                emptyList()
            } else {
                indexableCodes.filterIndexed { index, _ -> index >= nextGroupIndex }
            }
        }

        is NavigationIndex.Group -> {
            val groupIndex = indexableCodes.indexOf(navigationIndex.groupId)
            val nextGroupIndex =
                indexableCodes.indexOfFirst { code -> code.startsWith("G") && indexableCodes.indexOf(code) > groupIndex }
            if (nextGroupIndex == -1) {
                emptyList()
            } else {
                indexableCodes.filterIndexed { index, _ -> index >= nextGroupIndex }
            }
        }

        is NavigationIndex.Question -> {
            val questionIndex = indexableCodes.indexOf(navigationIndex.questionId)
            indexableCodes.filterIndexed { index, _ -> index > questionIndex }
        }

        is NavigationIndex.End -> emptyList()
    }
}

fun List<SurveyComponent>.indexableCodesRemoveDeprioritised(bindings: Map<Dependency, Any>): List<String> {
    return mapNotNull {
        if (it.noErrors() && it.hasUniqueCode()
            && (it !is Group || it.groupType != GroupType.END)
            && (!bindings.keys.contains(Dependency(it.code, Prioritised)) || bindings[Dependency(
                it.code,
                Prioritised
            )] as Boolean)
        )
            mutableListOf(it.code)
                .apply {
                    addAll(it.children.indexableCodes())
                }
        else
            null
    }
        .flatten()
}


fun Survey.allInOne(): NavigationIndex {
    return NavigationIndex.Groups(groups.filter { it.groupType != GroupType.END }.map { it.code })
}

private fun Survey.firstRelevant(
    navigationMode: NavigationMode,
    orderRelevanceBindings: Map<Dependency, Any>
): NavigationIndex {
    return when (navigationMode) {
        NavigationMode.ALL_IN_ONE -> allInOne()

        NavigationMode.GROUP_BY_GROUP -> {
            NavigationIndex.Group(groups.first { orderRelevanceBindings.isRelevant(it.code) }.code)
        }

        NavigationMode.QUESTION_BY_QUESTION -> {
            val firstRelevantGroup = groups.first { orderRelevanceBindings.isRelevant(it.code) }
            return NavigationIndex.Question(firstRelevantGroup.questions.first { orderRelevanceBindings.isRelevant(it.code) }.code)
        }
    }
}

private fun Survey.firstInvalid(
    navigationMode: NavigationMode,
    orderRelevanceBindings: Map<Dependency, Any>
): NavigationIndex {
    return when (navigationMode) {
        NavigationMode.ALL_IN_ONE -> {
            NavigationIndex.Groups(groups.filter { it.groupType != GroupType.END }.map { it.code })
        }

        NavigationMode.GROUP_BY_GROUP -> {
            NavigationIndex.Group(groups.first { orderRelevanceBindings.isInvalid(it.code) }.code)
        }

        NavigationMode.QUESTION_BY_QUESTION -> {
            val firstRelevantGroup = groups.first { orderRelevanceBindings.isInvalid(it.code) }
            return NavigationIndex.Question(firstRelevantGroup.questions.first { orderRelevanceBindings.isInvalid(it.code) }.code)
        }
    }
}

private fun Survey.nextRelevant(
    navigationInfo: NavigationInfo,
    navigationMode: NavigationMode,
    orderRelevanceBindings: Map<Dependency, Any>
): NavigationIndex {
    return when (navigationMode) {
        NavigationMode.ALL_IN_ONE -> {
            NavigationIndex.End(groups.last().code)
        }

        NavigationMode.GROUP_BY_GROUP -> {
            val groupId = (navigationInfo.navigationIndex as? NavigationIndex.Group)?.groupId ?: return firstRelevant(
                navigationMode,
                orderRelevanceBindings
            )
            val index = groups.indexOfFirst { it.code == groupId }
            val nextGroup = groups.subList(index + 1, groups.size).firstOrNull {
                orderRelevanceBindings.isRelevant(it.code) && it.hasRelevantChildren(orderRelevanceBindings)
            } ?: groups[index]
            if (nextGroup.groupType == GroupType.END || groups.indexOf(nextGroup) == index) {
                NavigationIndex.End(groups.last().code)
            } else {
                NavigationIndex.Group(nextGroup.code)
            }

        }

        NavigationMode.QUESTION_BY_QUESTION -> {
            val questionId =
                (navigationInfo.navigationIndex as? NavigationIndex.Question)?.questionId ?: return firstRelevant(
                    navigationMode,
                    orderRelevanceBindings
                )
            val group = getGroup(questionId)
            val questionIndex = group.questions.indexOfFirst { it.code == questionId }
            val nextQuestion = group.questions.subList(questionIndex + 1, group.questions.size)
                .firstOrNull { orderRelevanceBindings.isRelevant(it.code) }
            return if (nextQuestion != null) {
                NavigationIndex.Question(nextQuestion.code)
            } else {
                val groupIndex = groups.indexOfFirst { it.code == group.code }
                val nextGroup = groups.subList(groupIndex + 1, groups.size)
                    .firstOrNull {
                        orderRelevanceBindings.isRelevant(it.code) && it.hasRelevantChildren(
                            orderRelevanceBindings
                        )
                    } ?: groups[groupIndex]
                if (nextGroup.groupType == GroupType.END || groups.indexOf(nextGroup) == groupIndex) {
                    NavigationIndex.End(groups.last().code)
                } else {
                    NavigationIndex.Question(nextGroup.questions.first { orderRelevanceBindings.isRelevant(it.code) }.code)
                }
            }
        }
    }
}

fun SurveyComponent.hasRelevantChildren(orderRelevanceBindings: Map<Dependency, Any>): Boolean {
    return this.children.any { orderRelevanceBindings.isRelevant(it.code) }
}

private fun Survey.prevRelevant(
    navigationInfo: NavigationInfo,
    navigationMode: NavigationMode,
    orderRelevanceBindings: Map<Dependency, Any>
): NavigationIndex {
    return when (navigationMode) {
        NavigationMode.ALL_IN_ONE -> allInOne()

        NavigationMode.GROUP_BY_GROUP -> {
            val groupId = (navigationInfo.navigationIndex as? NavigationIndex.Group)?.groupId ?: return firstRelevant(
                navigationMode,
                orderRelevanceBindings
            )
            val index = children.indexOfFirst { it.code == groupId }
            val prevGroup = children.subList(0, index).lastOrNull {
                orderRelevanceBindings.isRelevant(it.code) && it.hasRelevantChildren(orderRelevanceBindings)
            }
            prevGroup?.let {
                NavigationIndex.Group(it.code)
            } ?: navigationInfo.navigationIndex

        }

        NavigationMode.QUESTION_BY_QUESTION -> {
            val questionId =
                (navigationInfo.navigationIndex as? NavigationIndex.Question)?.questionId ?: return firstRelevant(
                    navigationMode,
                    orderRelevanceBindings
                )
            val group = getGroup(questionId)
            val questionIndex = group.children.indexOfFirst { it.code == questionId }
            val prevQuestion = group.children.subList(0, questionIndex)
                .lastOrNull { orderRelevanceBindings.isRelevant(it.code) }
            return if (prevQuestion != null) {
                NavigationIndex.Question(prevQuestion.code)
            } else {
                val groupIndex = children.indexOfFirst { it.code == group.code }
                val nextGroup = children.subList(0, groupIndex)
                    .lastOrNull {
                        orderRelevanceBindings.isRelevant(it.code) && it.hasRelevantChildren(
                            orderRelevanceBindings
                        )
                    }
                    ?: return navigationInfo.navigationIndex
                NavigationIndex.Question(nextGroup.children.last { orderRelevanceBindings.isRelevant(it.code) }.code)
            }
        }
    }
}

private fun Map<Dependency, Any>.isRelevant(code: String): Boolean {
    return get(Dependency(code, Relevance)) as? Boolean ?: true
}

private fun Map<Dependency, Any>.isInvalid(code: String): Boolean {
    return get(Dependency(code, Validity)) as? Boolean == false
}

private fun Survey.getGroup(questionCode: String): Group {
    return children.first { group ->
        group.children.any { it.code == questionCode }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun Survey.sortByOrder(orderBindings: Map<Dependency, Any>): Survey {
    return copy(groups = (groups.toMutableList() as MutableList<SurveyComponent>)
        .apply { sortByOrder(code, orderBindings) } as List<Group>)
}

internal fun MutableList<SurveyComponent>.sortByOrder(
    parentCode: String = "",
    orderBindings: Map<Dependency, Any>
) {
    sortBy { surveyComponent ->
        val code = surveyComponent.uniqueCode(parentCode)
        if (orderBindings.containsKey(Dependency(code, Order))) {
            (orderBindings[Dependency(code, Order)] as Number).toInt()
        } else {
            indexOf(surveyComponent) + 1
        }
    }
    forEachIndexed { index, surveyComponent ->
        val code = surveyComponent.uniqueCode(parentCode)
        set(index, surveyComponent.duplicate(children = surveyComponent.children.toMutableList().apply {
            sortByOrder(code, orderBindings)
        }))
    }
}