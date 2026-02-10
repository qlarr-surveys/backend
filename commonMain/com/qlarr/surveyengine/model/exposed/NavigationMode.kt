package com.qlarr.surveyengine.model.exposed

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class NavigationMode {
    ALL_IN_ONE,
    GROUP_BY_GROUP,
    QUESTION_BY_QUESTION;

    companion object {
        fun fromString(string: String?) = when (string?.lowercase()) {
            ALL_IN_ONE.name.lowercase() -> ALL_IN_ONE
            QUESTION_BY_QUESTION.name.lowercase() -> QUESTION_BY_QUESTION
            else -> GROUP_BY_GROUP
        }
    }
}