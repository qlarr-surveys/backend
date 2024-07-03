package com.frankie.backend.api.runsurvey

import com.frankie.expressionmanager.model.NavigationDirection
import com.frankie.expressionmanager.model.ResponseEvent
import java.util.*

data class StartRequest(
    val lang: String? = null,
    val values: Map<String, Any> = mapOf()
)

data class NavigateRequest(
    val responseId: UUID,
    val lang: String? = null,
    val navigationDirection: NavigationDirection,
    val events: List<ResponseEvent> = listOf(),
    val values: Map<String, Any> = mapOf()
)