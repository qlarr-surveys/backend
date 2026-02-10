package com.qlarr.backend.api.response

data class AnalyticsDto(
    val surveyTitle: String,
    val totalResponses: Int,
    val questions: List<AnalyticsQuestion>
)

data class AnalyticsQuestion(
    val id: String,
    val type: String,
    val title: String,
    val description: String? = null,
    val options: List<String>? = null,
    val rows: List<String>? = null,
    val columns: List<String>? = null,
    val images: List<AnalyticsImage>? = null,
    val fields: List<String>? = null,
    val responses: List<Any?> = emptyList()
)

data class AnalyticsImage(
    val id: String,
    val label: String? = null,
    val url: String? = null
)
