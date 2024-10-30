package com.qlarr.backend.api.runsurvey

import com.qlarr.surveyengine.model.NavigationDirection
import java.util.*

data class StartRequest(
    val lang: String? = null,
    val values: Map<String, Any> = mapOf()
)

data class NavigateRequest(
    val responseId: UUID,
    val lang: String? = null,
    val navigationDirection: NavigationDirection,
    val values: Map<String, Any> = mapOf()
)