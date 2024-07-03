package com.frankie.expressionmanager.context.execute

import com.frankie.expressionmanager.model.*

@Suppress("UNCHECKED_CAST")
internal fun Survey.sanitize(): Survey {
    return copy(groups = children.sanitize() as List<Group>)
}

internal fun List<SurveyComponent>.sanitize(): List<SurveyComponent> {
    return filter { surveyComponent ->
        surveyComponent.noErrors()
    }.map { surveyComponent ->
        val newInstructions = surveyComponent.instructionList.filterNoErrors()
        val newChildren = surveyComponent.children.sanitize()
        surveyComponent.duplicate(instructionList = newInstructions, children = newChildren)
    }
}

