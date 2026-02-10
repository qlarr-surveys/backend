package com.qlarr.surveyengine.model.exposed

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class SurveyMode {
    OFFLINE, ONLINE
}