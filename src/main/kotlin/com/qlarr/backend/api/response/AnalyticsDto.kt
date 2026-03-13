package com.qlarr.backend.api.response

data class AnalyticsDto(
    val surveyTitle: String,
    val totalResponses: Int,
    val incompleteResponses: Int,
    val previewResponses: Int,
    val questions: List<AnalyticsQuestion>
)

/**
 * Represents a single survey question with its aggregated response data.
 *
 * Type values match the frontend question types (uppercased).
 *
 * The [options], [rows], [columns], and [fields] lists provide code-to-label mappings
 * via [AnalyticsOption]. Response values contain raw answer codes (e.g., "A1") —
 * the frontend performs label resolution using these mappings.
 *
 * The [responses] list shape varies by [type]:
 * - **SCQ, AUTOCOMPLETE, IMAGE_SCQ, ICON_SCQ**: `List<String>` — each entry is an answer code (e.g., "A1").
 * - **MCQ, IMAGE_MCQ, ICON_MCQ**: `List<List<String>>` — each entry is a list of answer codes.
 * - **NPS**: `List<Number>` — raw numeric values (0–10).
 * - **RANKING, IMAGE_RANKING**: `List<List<String>>` — answer codes sorted by rank (1st to last).
 * - **SCQ_ARRAY, MCQ_ARRAY, SCQ_ICON_ARRAY, MCQ_ICON_ARRAY**: `List<Map<String, Any>>` — each entry maps row code to column code(s).
 * - **MULTIPLE_TEXT**: `List<Map<String, Any>>` — each entry maps field code to its value.
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
    val options: List<AnalyticsOption>? = null,
    val rows: List<AnalyticsOption>? = null,
    val columns: List<AnalyticsOption>? = null,
    val images: List<AnalyticsImage>? = null,
    val fields: List<AnalyticsOption>? = null,
    val responses: List<Any?> = emptyList()
)

data class AnalyticsOption(
    val code: String,
    val label: String
)

data class AnalyticsImage(
    val id: String,
    val label: String? = null,
    val url: String? = null
)
