package com.frankie.expressionmanager.navigate

import com.frankie.expressionmanager.model.NavigationIndex
import com.frankie.expressionmanager.context.nestedComponents
import com.frankie.expressionmanager.model.Survey
import java.lang.IllegalStateException

fun Survey.reduce(navigationIndex: NavigationIndex): Survey {
    return when (navigationIndex) {
        is NavigationIndex.Groups -> reduceComponentsToGroups(navigationIndex.groupIds)
        is NavigationIndex.Question -> reduceComponentsToQuestion(navigationIndex.questionId)
        is NavigationIndex.Group -> reduceComponentsToGroups(listOf(navigationIndex.groupId))
        is NavigationIndex.End -> reduceComponentsToGroups(listOf(navigationIndex.groupId))
    }
}



private fun Survey.reduceComponentsToGroups(groupCodes: List<String>): Survey {
    val newChildren = groups.filter { it.code in groupCodes }
    return copy(groups = newChildren)
}


private fun Survey.reduceComponentsToQuestion(questionCode: String): Survey {
    val nestedComponents = nestedComponents()
    val questionIndex = nestedComponents.indexOfFirst { it.code == questionCode }
    val parentGroupCode = nestedComponents.subList(0, questionIndex).last { it.code.startsWith("G") }.code
    val group = groups.first { it.code == parentGroupCode }
    val newGroupChildren = group.children.filter { it.code == questionCode }
    return copy(groups = listOf(group.copy(questions = newGroupChildren)))
}