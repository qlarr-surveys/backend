package com.qlarr.surveyengine.model

import com.qlarr.surveyengine.ext.VALID_ANSWER_CODE
import com.qlarr.surveyengine.ext.VALID_GROUP_CODE
import com.qlarr.surveyengine.ext.VALID_QUESTION_CODE
import com.qlarr.surveyengine.ext.VALID_SURVEY_CODE
import kotlinx.serialization.Serializable


enum class SurveyElementType(val codeRegex: String) {
    ANSWER(VALID_ANSWER_CODE),
    QUESTION(VALID_QUESTION_CODE),
    GROUP(VALID_GROUP_CODE),
    SURVEY(VALID_SURVEY_CODE);

    fun childType(): SurveyElementType = when (this) {
        ANSWER -> ANSWER
        QUESTION -> ANSWER
        GROUP -> QUESTION
        SURVEY -> GROUP
    }

    fun nameAsChildList(): String = when (this) {
        ANSWER -> "answers"
        QUESTION -> "questions"
        GROUP -> "groups"
        SURVEY -> throw IllegalStateException("cannot be child")
    }

    fun hasUniqueCode(): Boolean = when (this) {
        ANSWER -> false
        else -> true
    }
}





