package com.qlarr.backend.api.runsurvey

import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.backend.api.response.ResponseEvent
import com.qlarr.surveyengine.model.exposed.NavigationMode
import java.time.LocalDateTime
import java.util.*

data class StartRequest(
    val lang: String? = null,
    val navigationMode: NavigationMode? = null,
    val values: Map<String, Any> = mapOf(),
    val clientUTCTime: LocalDateTime? = null
)

data class NavigateRequest(
    val responseId: UUID,
    val lang: String? = null,
    val navigationMode: NavigationMode? = null,
    val navigationDirection: NavigationDirection,
    val values: Map<String, Any> = mapOf(),
    val events: List<ResponseEvent> = listOf(),
    val clientUTCTime: LocalDateTime? = null
)