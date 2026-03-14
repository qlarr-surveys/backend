package com.qlarr.backend.api.response

import com.fasterxml.jackson.annotation.JsonInclude

data class AnalyticsDto(
    val totalResponses: Int,
    val incompleteResponses: Int,
    val questions: List<AnalyticsQuestion>
)

/**
 * Represents a single survey question with its response data.
 *
 * Type values match the frontend question types (uppercased).
 *
 * The [options], [rows], [columns], and [fields] lists provide code-to-label mappings
 * via [AnalyticsOption].
 *
 * Depending on [type], one of the summary fields is populated:
 * - **SCQ, AUTOCOMPLETE, IMAGE_SCQ, ICON_SCQ, MCQ, IMAGE_MCQ, ICON_MCQ**: [frequencyCounts] — count per option code.
 * - **NPS**: [npsSummary] — detractors, passives, promoters, and NPS score.
 * - **NUMBER**: [numberSummary] — min, max, mean, median, sum, count.
 * - **RANKING, IMAGE_RANKING**: [rankingSummary] — average rank per option.
 * - **SCQ_ARRAY, MCQ_ARRAY, SCQ_ICON_ARRAY, MCQ_ICON_ARRAY**: [matrixSummary] — count per row-column pair.
 * - **SIGNATURE, PHOTO_CAPTURE**: [presenceCount] — how many respondents submitted.
 *
 * For types that cannot be meaningfully aggregated, [responses] contains raw values:
 * - **TEXT, PARAGRAPH, EMAIL**: `List<String>` — raw text values.
 * - **DATE, TIME, DATE_TIME**: `List<String>` — formatted date/time strings.
 * - **MULTIPLE_TEXT**: `List<Map<String, Any>>` — each entry maps field code to its value.
 * - **FILE_UPLOAD, BARCODE**: `List<Any>` — raw values as stored.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnalyticsQuestion(
    val id: String,
    val type: String,
    val title: String,
    val answeredCount: Int = 0,
    val options: List<AnalyticsOption>? = null,
    val rows: List<AnalyticsOption>? = null,
    val columns: List<AnalyticsOption>? = null,
    val images: List<AnalyticsImage>? = null,
    val fields: List<AnalyticsOption>? = null,
    val frequencyCounts: List<FrequencyCount>? = null,
    val npsSummary: NpsSummary? = null,
    val numberSummary: NumberSummary? = null,
    val rankingSummary: List<RankingSummaryItem>? = null,
    val matrixSummary: List<MatrixSummaryItem>? = null,
    val presenceCount: PresenceCount? = null,
    val responses: List<Any?>? = null
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

data class FrequencyCount(
    val code: String,
    val count: Int
)

data class NpsSummary(
    val detractors: Int,
    val passives: Int,
    val promoters: Int,
    val score: Double,
    val answeredCount: Int,
    val distribution: List<Int>
)

data class NumberSummary(
    val min: Double,
    val max: Double,
    val mean: Double,
    val median: Double,
    val sum: Double,
    val count: Int,
    val stdDev: Double,
    val frequencyTable: List<NumberFrequencyItem>,
    val outlierValues: List<Double>,
    val outliersCount: Int
)

data class NumberFrequencyItem(
    val value: Double,
    val count: Int
)

data class RankingSummaryItem(
    val code: String,
    val averageRank: Double,
    val responseCount: Int,
    val firstPlaceCount: Int,
    val lastPlaceCount: Int
)

data class MatrixSummaryItem(
    val rowCode: String,
    val columnCode: String,
    val count: Int
)

data class PresenceCount(
    val presentCount: Int,
    val totalResponses: Int
)
