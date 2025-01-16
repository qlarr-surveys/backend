package com.qlarr.backend.api.runsurvey

import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.surveyengine.model.NavigationIndex
import com.qlarr.surveyengine.model.SurveyLang
import java.util.*

data class RunSurveyDto(
    val survey: ObjectNode,
    val state: ObjectNode,
    val navigationIndex: NavigationIndex,
    val responseId: UUID,
    val lang: SurveyLang,
    val additionalLang: List<SurveyLang>
)