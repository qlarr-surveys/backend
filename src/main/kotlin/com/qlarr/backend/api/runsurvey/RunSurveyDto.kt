package com.qlarr.backend.api.runsurvey

import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.persistence.entities.SurveyNavigationData
import com.qlarr.surveyengine.model.SurveyLang
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import java.util.*

data class RunSurveyDto(
    val survey: ObjectNode,
    val state: ObjectNode,
    val navigationData: SurveyNavigationData,
    val navigationIndex: NavigationIndex,
    val responseId: UUID,
    val lang: SurveyLang,
    val additionalLang: List<SurveyLang>
)