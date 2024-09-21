package com.qlarr.backend.api.runsurvey

import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.expressionmanager.model.NavigationIndex
import com.qlarr.expressionmanager.model.SurveyLang
import java.util.*

data class RunSurveyDto(
    val survey: ObjectNode,
    val state: ObjectNode,
    val navigationIndex: NavigationIndex,
    val responseId: UUID,
    val lang: SurveyLang,
    val additionalLang: List<SurveyLang>
)