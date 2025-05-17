package com.qlarr.backend.api.runsurvey

import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.surveyengine.model.exposed.NavigationMode
import java.util.*

data class StartRequest(
    val lang: String? = null,
    val navigationMode: NavigationMode? = null,
    val values: Map<String, Any> = mapOf()
)

data class NavigateRequest(
    val responseId: UUID,
    val lang: String? = null,
    val navigationDirection: NavigationDirection,
    val values: Map<String, Any> = mapOf()
)