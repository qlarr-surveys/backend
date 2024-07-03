package com.frankie.expressionmanager.model

import com.frankie.expressionmanager.ext.VALID_ANSWER_CODE
import com.frankie.expressionmanager.ext.VALID_GROUP_CODE
import com.frankie.expressionmanager.ext.VALID_QUESTION_CODE
import com.frankie.expressionmanager.ext.VALID_SURVEY_CODE
import com.frankie.expressionmanager.model.SurveyElementType.*


enum class SurveyElementType(val codeRegex: String) {
    ANSWER(VALID_ANSWER_CODE),
    QUESTION(VALID_QUESTION_CODE),
    GROUP(VALID_GROUP_CODE),
    SURVEY(VALID_SURVEY_CODE);
}

fun SurveyElementType.childType(): SurveyElementType = when (this) {
    ANSWER -> ANSWER
    QUESTION -> ANSWER
    GROUP -> QUESTION
    SURVEY -> GROUP
}

fun SurveyElementType.nameAsChildList(): String = when (this) {
    ANSWER -> "answers"
    QUESTION -> "questions"
    GROUP -> "groups"
    SURVEY -> throw IllegalStateException("cannot be child")
}

fun SurveyElementType.hasUniqueCode(): Boolean = when (this) {
    ANSWER -> false
    else -> true
}
