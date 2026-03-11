package com.qlarr.backend.api.response

data class AnalyticsDto(
    val surveyTitle: String,
    val totalResponses: Int,
    val questions: List<AnalyticsQuestion>
)

/**
 * Represents a single survey question with its aggregated response data.
 *
 * Type values match the frontend question types (uppercased).
 *
 * The [responses] list shape varies by [type]:
 * - **SCQ, AUTOCOMPLETE, IMAGE_SCQ, ICON_SCQ**: `List<String>` — each entry is the chosen option label.
 * - **MCQ, IMAGE_MCQ, ICON_MCQ**: `List<List<String>>` — each entry is a list of selected option labels.
 * - **NPS**: `List<Number>` — raw numeric values (0–10).
 * - **RANKING, IMAGE_RANKING**: `List<List<String>>` — each entry is options sorted by rank (1st to last).
 * - **SCQ_ARRAY, MCQ_ARRAY, SCQ_ICON_ARRAY, MCQ_ICON_ARRAY**: `List<Map<String, Any>>` — each entry maps row label to column label(s).
 * - **MULTIPLE_TEXT**: `List<Map<String, Any>>` — each entry maps field label to its value.
 * - **NUMBER**: `List<Number>` — raw numeric values.
 * - **TEXT, PARAGRAPH, EMAIL**: `List<String>` — raw text values.
 * - **DATE, TIME, DATE_TIME**: `List<String>` — formatted date/time strings.
 * - **SIGNATURE, PHOTO_CAPTURE**: `List<Boolean>` — `true` indicates presence (submitted).
 * - **FILE_UPLOAD, BARCODE**: `List<Any>` — raw values as stored.
 */
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
