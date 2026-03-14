package com.qlarr.backend.services

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.qlarr.backend.api.response.*
import com.qlarr.backend.common.stripHtmlTags
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.model.ComponentIndex
import com.qlarr.surveyengine.model.exposed.ColumnName
import com.qlarr.surveyengine.model.exposed.ResponseField
import com.qlarr.surveyengine.model.exposed.ReturnType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class AnalyticsService(
    private val designService: DesignService,
    private val responseRepository: ResponseRepository
) {
    private val logger = LoggerFactory.getLogger(AnalyticsService::class.java)

    companion object {
        private val CHOICE_TYPES = setOf(
            "SCQ", "MCQ", "RANKING", "IMAGE_RANKING", "AUTOCOMPLETE",
            "ICON_SCQ", "ICON_MCQ", "IMAGE_SCQ", "IMAGE_MCQ", "NPS"
        )
        private val MATRIX_TYPES = setOf("SCQ_ARRAY", "MCQ_ARRAY", "SCQ_ICON_ARRAY", "MCQ_ICON_ARRAY")
        private val RANKING_TYPES = setOf("RANKING", "IMAGE_RANKING")
        private val ICON_IMAGE_CHOICE_TYPES = setOf("ICON_SCQ", "ICON_MCQ", "IMAGE_SCQ", "IMAGE_MCQ")
        private val MULTI_FIELD_TYPES = setOf("MULTIPLE_TEXT")
        private val SINGLE_CHOICE_TYPES = setOf("SCQ", "AUTOCOMPLETE", "IMAGE_SCQ", "ICON_SCQ")
        private val MULTI_CHOICE_TYPES = setOf("MCQ", "IMAGE_MCQ", "ICON_MCQ")
        private val PRESENCE_ONLY_TYPES = setOf("SIGNATURE", "PHOTO_CAPTURE")
        private const val NPS_TYPE = "NPS"
        private const val NUMBER_TYPE = "NUMBER"
        private val CHILD_KEYS = listOf("children", "groups", "questions", "answers")
        private val ROW_REGEX = Regex("^A\\d+$")
        private val COLUMN_REGEX = Regex("^Ac\\d+$")
        const val DEFAULT_MAX_RESPONSES = 5000

        private fun String.isQuestionCode(): Boolean = startsWith("Q") && !contains("A")
        private fun String.fileNameWithoutExtension(): String =
            substringAfterLast("/").substringBeforeLast(".")

        private fun Double.roundTo2(): Double = Math.round(this * 100.0) / 100.0
    }

    private data class AnalyticsContext(
        val labels: Map<String, String>,
        val schemaMap: Map<String, ResponseField>,
        val componentIndexList: List<ComponentIndex>,
        val questionTypes: Map<String, String>,
        val resources: Map<String, String>,
        val surveyId: UUID,
        val responses: List<Map<String, Any>>
    )

    fun getAnalytics(surveyId: UUID, maxResponses: Int = DEFAULT_MAX_RESPONSES): AnalyticsDto {
        val processed = designService.getProcessedSurvey(surveyId, published = false)
        val survey = processed.survey
        val validationOutput = processed.validationJsonOutput

        val labels = validationOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
        val schemaMap = validationOutput.schema
            .filter { it.columnName == ColumnName.VALUE }
            .associateBy { it.componentCode }

        // Extract question types and content paths in a single tree traversal
        val (questionTypes, contentPaths) = extractQuestionMetadata(validationOutput.survey)

        // Fetch completed response values only (skip events, nav_index, etc.)
        val responses = responseRepository.findCompletedValuesBySurveyId(surveyId, maxResponses)
            .map { objectMapper.readValue(it, jacksonTypeRef<Map<String, Any>>()) }

        val ctx = AnalyticsContext(
            labels,
            schemaMap,
            validationOutput.componentIndexList,
            questionTypes,
            contentPaths,
            surveyId,
            responses
        )

        // Build analytics questions
        val questionCodes = ctx.componentIndexList
            .map { it.code }
            .filter { it.isQuestionCode() }

        val questions = questionCodes.mapNotNull { buildAnalyticsQuestion(it, ctx) }

        val counts = responseRepository.analyticsResponseCounts(surveyId)

        return AnalyticsDto(
            surveyTitle = survey.name,
            totalResponses = counts.completedCount + counts.incompleteCount + counts.previewCount,
            incompleteResponses = counts.incompleteCount,
            previewResponses = counts.previewCount,
            questions = questions
        )
    }

    // --- Tree traversal ---

    private fun traverseSurveyTree(
        node: ObjectNode,
        parentQuestionCode: String? = null,
        visitor: (node: ObjectNode, code: String?, parentQuestionCode: String?) -> Unit
    ) {
        val code = node.get("code")?.asText()
        visitor(node, code, parentQuestionCode)

        val currentQuestionCode = if (code != null && code.isQuestionCode()) code else parentQuestionCode
        CHILD_KEYS.forEach { childKey ->
            node.get(childKey)?.forEach { child ->
                if (child is ObjectNode) {
                    traverseSurveyTree(child, currentQuestionCode, visitor)
                }
            }
        }
    }

    private fun extractQuestionMetadata(survey: ObjectNode): Pair<Map<String, String>, Map<String, String>> {
        val types = mutableMapOf<String, String>()
        val contentPaths = mutableMapOf<String, String>()
        traverseSurveyTree(survey) { node, code, parentQuestionCode ->
            if (code != null && code.isQuestionCode()) {
                node.get("type")?.asText()?.let { types[code] = it.uppercase() }
            }
            if (code != null && code.startsWith("A") && parentQuestionCode != null) {
                resolveContentPaths(node)?.let { contentPaths[parentQuestionCode + code] = it }
            }
        }
        return types to contentPaths
    }


    private fun resolveContentPaths(node: ObjectNode): String? {
        return (node.get("resources") as? ObjectNode)?.let { resourcesNode->
            resourcesNode.get("icon")?.asText() ?: resourcesNode.get("image")?.asText()
        }
    }

    private fun isEmptyValue(value: Any?): Boolean = when (value) {
        null -> true
        is String -> value.isBlank()
        is List<*> -> value.isEmpty()
        is Map<*, *> -> value.isEmpty()
        else -> false
    }

    private fun inferTypeFromReturnType(dataType: ReturnType): String {
        return when (dataType) {
            is ReturnType.Enum -> "SCQ"
            is ReturnType.List -> "MCQ"
            ReturnType.Int -> "NUMBER"
            ReturnType.String -> "TEXT"
            ReturnType.Date -> "DATE"
            ReturnType.Map -> "SCQ_ARRAY"
            ReturnType.Boolean -> "SCQ"
            ReturnType.Double -> "NUMBER"
            ReturnType.File -> "FILE_UPLOAD"
        }
    }

    // --- Analytics question building ---

    private fun buildAnalyticsQuestion(questionCode: String, ctx: AnalyticsContext): AnalyticsQuestion? {
        val responseField = ctx.schemaMap[questionCode]
        val questionType = ctx.questionTypes[questionCode]
            ?: responseField?.let { inferTypeFromReturnType(it.dataType) }
            ?: return null
        val title = ctx.labels[questionCode] ?: questionCode

        // Get answer codes (children of this question)
        val componentIndex = ctx.componentIndexList.firstOrNull { it.code == questionCode }
        val answerCodes = componentIndex?.children ?: emptyList()

        val options = if (questionType in CHOICE_TYPES) {
            toAnalyticsOptions(answerCodes, questionCode, ctx.labels)
        } else null

        // Extract response values for this question
        val isMatrix = questionType in MATRIX_TYPES
        val isRanking = questionType in RANKING_TYPES
        val responseValues: List<Any?> = if (isRanking) {
            extractRankingFromAnswerValues(ctx, answerCodes, questionCode)
        } else if (responseField != null) {
            extractResponses(questionType, responseField.toValueKey(), questionCode, ctx)
        } else if (isMatrix) {
            extractMatrixMultiFieldResponses(ctx, answerCodes, questionCode)
        } else {
            extractMultiFieldResponses(ctx, answerCodes, questionCode)
        }

        val rows = if (isMatrix) {
            toAnalyticsOptions(answerCodes, questionCode, ctx.labels) { it.matches(ROW_REGEX) }
        } else null

        val columns = if (isMatrix) {
            toAnalyticsOptions(answerCodes, questionCode, ctx.labels) { it.matches(COLUMN_REGEX) }
        } else if (questionType in ICON_IMAGE_CHOICE_TYPES) {
            toAnalyticsOptions(answerCodes, questionCode, ctx.labels)
        } else null

        val images = answerCodes.mapNotNull { answerCode ->
            ctx.resources[answerCode]?.let { resourceFile ->
                AnalyticsImage(
                    id = answerCode,
                    label = ctx.labels[answerCode],
                    url = buildResourceUrl(ctx.surveyId, resourceFile)
                )
            }
        }.ifEmpty { null }

        val fields = if (questionType in MULTI_FIELD_TYPES) {
            toAnalyticsOptions(answerCodes, questionCode, ctx.labels)
        } else null

        val base = AnalyticsQuestion(
            id = questionCode,
            type = questionType,
            title = title,
            answeredCount = responseValues.size,
            options = options,
            rows = rows,
            columns = columns,
            images = images,
            fields = fields
        )

        return when {
            questionType == NPS_TYPE -> base.copy(npsSummary = aggregateNps(responseValues))
            questionType == NUMBER_TYPE -> base.copy(numberSummary = aggregateNumber(responseValues))
            questionType in SINGLE_CHOICE_TYPES -> base.copy(
                frequencyCounts = aggregateFrequencyCounts(
                    responseValues,
                    options!!,
                    isSingleChoice = true
                )
            )

            questionType in MULTI_CHOICE_TYPES -> base.copy(
                frequencyCounts = aggregateFrequencyCounts(
                    responseValues,
                    options!!,
                    isSingleChoice = false
                )
            )

            questionType in RANKING_TYPES -> base.copy(rankingSummary = aggregateRanking(responseValues, options!!))
            questionType in MATRIX_TYPES -> base.copy(
                matrixSummary = aggregateMatrix(
                    responseValues,
                    rows!!,
                    columns!!,
                    questionType
                )
            )

            questionType in PRESENCE_ONLY_TYPES -> base.copy(
                presenceCount = PresenceCount(
                    presentCount = responseValues.size,
                    totalResponses = ctx.responses.size
                )
            )

            else -> base.copy(responses = responseValues)
        }
    }

    private fun buildResourceUrl(surveyId: UUID, fileName: String): String {
        return "/survey/$surveyId/resource/$fileName"
    }

    private fun toAnalyticsOptions(
        answerCodes: List<String>,
        questionCode: String,
        labels: Map<String, String>,
        filter: ((String) -> Boolean)? = null
    ): List<AnalyticsOption> {
        val codes = if (filter != null) answerCodes.filter { filter(it.removePrefix(questionCode)) } else answerCodes
        return codes.map { fullCode ->
            val stripped = fullCode.removePrefix(questionCode)
            AnalyticsOption(code = stripped, label = labels[fullCode] ?: stripped)
        }
    }

    // --- Response extraction ---

    private fun extractResponses(
        type: String,
        valueKey: String,
        questionCode: String,
        ctx: AnalyticsContext
    ): List<Any?> {
        return ctx.responses.mapNotNull { response ->
            val value = response[valueKey] ?: return@mapNotNull null
            if (isEmptyValue(value)) return@mapNotNull null
            when {
                type in SINGLE_CHOICE_TYPES -> value.toString()
                type in MULTI_CHOICE_TYPES -> {
                    val list = value as? List<*>
                    if (list == null) {
                        logger.warn("Expected List for MCQ question {}, got {}", questionCode, value::class.simpleName)
                        null
                    } else {
                        list.map { it.toString() }
                    }
                }

                type in PRESENCE_ONLY_TYPES -> true
                else -> value
            }
        }
    }

    private fun extractMatrixMultiFieldResponses(
        ctx: AnalyticsContext,
        answerCodes: List<String>,
        questionCode: String
    ): List<Any?> {
        val rowCodes = answerCodes.filter {
            it.removePrefix(questionCode).matches(ROW_REGEX)
        }
        return ctx.responses.mapNotNull { response ->
            val fieldMap = rowCodes.mapNotNull mapField@{ answerCode ->
                val field = ctx.schemaMap[answerCode] ?: return@mapField null
                val value = response[field.toValueKey()] ?: return@mapField null
                if (isEmptyValue(value)) return@mapField null
                answerCode.removePrefix(questionCode) to value
            }.toMap()
            fieldMap.ifEmpty { null }
        }
    }

    private fun extractRankingFromAnswerValues(
        ctx: AnalyticsContext,
        answerCodes: List<String>,
        questionCode: String
    ): List<Any?> {
        return ctx.responses.mapNotNull { response ->
            val rankedItems = answerCodes.mapNotNull mapField@{ answerCode ->
                val field = ctx.schemaMap[answerCode] ?: return@mapField null
                val value = response[field.toValueKey()] ?: return@mapField null
                val rank = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: return@mapField null
                    else -> return@mapField null
                }
                rank to answerCode.removePrefix(questionCode)
            }
            if (rankedItems.isEmpty()) null
            else rankedItems.sortedBy { it.first }.map { it.second }
        }
    }

    private fun extractMultiFieldResponses(
        ctx: AnalyticsContext,
        answerCodes: List<String>,
        questionCode: String
    ): List<Any?> {
        return ctx.responses.mapNotNull { response ->
            val fieldMap = answerCodes.mapNotNull mapField@{ answerCode ->
                val field = ctx.schemaMap[answerCode] ?: return@mapField null
                val value = response[field.toValueKey()] ?: return@mapField null
                if (isEmptyValue(value)) return@mapField null
                answerCode.removePrefix(questionCode) to value
            }.toMap()
            fieldMap.ifEmpty { null }
        }
    }

    // --- Aggregation methods ---

    private fun aggregateFrequencyCounts(
        responses: List<Any?>,
        options: List<AnalyticsOption>,
        isSingleChoice: Boolean
    ): List<FrequencyCount> {
        val counts = options.associate { it.code to 0 }.toMutableMap()
        if (isSingleChoice) {
            responses.forEach { value ->
                val code = value?.toString() ?: return@forEach
                counts[code] = (counts[code] ?: 0) + 1
            }
        } else {
            responses.forEach { value ->
                val selections = value as? List<*> ?: return@forEach
                selections.forEach { code ->
                    val key = code?.toString() ?: return@forEach
                    counts[key] = (counts[key] ?: 0) + 1
                }
            }
        }
        return options.map { FrequencyCount(code = it.code, count = counts[it.code] ?: 0) }
    }

    private fun aggregateNps(responses: List<Any?>): NpsSummary {
        val numbers = responses.mapNotNull { (it as? Number)?.toInt() }
        val detractors = numbers.count { it in 0..6 }
        val passives = numbers.count { it in 7..8 }
        val promoters = numbers.count { it in 9..10 }
        val total = numbers.size
        val score = if (total > 0) (promoters - detractors).toDouble() / total * 100 else 0.0
        val distribution = IntArray(11)
        numbers.forEach { if (it in 0..10) distribution[it]++ }
        return NpsSummary(
            detractors = detractors,
            passives = passives,
            promoters = promoters,
            score = score,
            answeredCount = total,
            distribution = distribution.toList()
        )
    }

    private fun aggregateNumber(responses: List<Any?>): NumberSummary? {
        val numbers = responses.mapNotNull { (it as? Number)?.toDouble() }
        if (numbers.isEmpty()) return null
        val sorted = numbers.sorted()
        val count = sorted.size
        val mean = numbers.average()
        val median = if (count % 2 == 0) {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        } else {
            sorted[count / 2]
        }

        // Standard deviation
        val variance = numbers.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        // Frequency table
        val freqMap = mutableMapOf<Double, Int>()
        numbers.forEach { freqMap[it] = (freqMap[it] ?: 0) + 1 }
        val frequencyTable = freqMap.entries
            .sortedByDescending { it.value }
            .map { NumberFrequencyItem(value = it.key, count = it.value) }

        // Outlier detection (IQR method)
        val outlierValues = if (count >= 4) {
            val mid = count / 2
            val q1Arr = sorted.subList(0, mid)
            val q3Arr = if (count % 2 != 0) sorted.subList(mid + 1, count) else sorted.subList(mid, count)
            val q1 = if (q1Arr.size % 2 == 0) {
                (q1Arr[q1Arr.size / 2 - 1] + q1Arr[q1Arr.size / 2]) / 2.0
            } else {
                q1Arr[q1Arr.size / 2]
            }
            val q3 = if (q3Arr.size % 2 == 0) {
                (q3Arr[q3Arr.size / 2 - 1] + q3Arr[q3Arr.size / 2]) / 2.0
            } else {
                q3Arr[q3Arr.size / 2]
            }
            val iqr = q3 - q1
            val lowerBound = q1 - 1.5 * iqr
            val upperBound = q3 + 1.5 * iqr
            numbers.filter { it < lowerBound || it > upperBound }
        } else {
            emptyList()
        }

        return NumberSummary(
            min = sorted.first(),
            max = sorted.last(),
            mean = mean.roundTo2(),
            median = median.roundTo2(),
            sum = numbers.sum().roundTo2(),
            count = count,
            stdDev = stdDev.roundTo2(),
            frequencyTable = frequencyTable,
            outlierValues = outlierValues,
            outliersCount = outlierValues.size
        )
    }

    private fun aggregateRanking(
        responses: List<Any?>,
        options: List<AnalyticsOption>
    ): List<RankingSummaryItem> {
        val rankLists = options.associate { it.code to mutableListOf<Int>() }
        val firstPlaceCounts = options.associate { it.code to 0 }.toMutableMap()
        val lastPlaceCounts = options.associate { it.code to 0 }.toMutableMap()
        responses.forEach { value ->
            val ranked = value as? List<*> ?: return@forEach
            ranked.forEachIndexed { index, code ->
                val key = code?.toString() ?: return@forEachIndexed
                rankLists[key]?.add(index + 1)
                if (index == 0) firstPlaceCounts[key] = (firstPlaceCounts[key] ?: 0) + 1
                if (index == ranked.size - 1) lastPlaceCounts[key] = (lastPlaceCounts[key] ?: 0) + 1
            }
        }
        return options.map { option ->
            val ranks = rankLists[option.code] ?: emptyList()
            RankingSummaryItem(
                code = option.code,
                averageRank = if (ranks.isNotEmpty()) ranks.average().roundTo2() else 0.0,
                responseCount = ranks.size,
                firstPlaceCount = firstPlaceCounts[option.code] ?: 0,
                lastPlaceCount = lastPlaceCounts[option.code] ?: 0
            )
        }
    }

    private fun aggregateMatrix(
        responses: List<Any?>,
        rows: List<AnalyticsOption>,
        columns: List<AnalyticsOption>,
        questionType: String
    ): List<MatrixSummaryItem> {
        val counts = mutableMapOf<Pair<String, String>, Int>()
        rows.forEach { row ->
            columns.forEach { col ->
                counts[row.code to col.code] = 0
            }
        }
        val isMultiChoice = questionType in setOf("MCQ_ARRAY", "MCQ_ICON_ARRAY")
        responses.forEach { value ->
            val rowMap = value as? Map<*, *> ?: return@forEach
            rowMap.forEach { (rowCode, colValue) ->
                val rCode = rowCode?.toString() ?: return@forEach
                if (isMultiChoice) {
                    val selections = colValue as? List<*> ?: return@forEach
                    selections.forEach { col ->
                        val cCode = col?.toString() ?: return@forEach
                        val key = rCode to cCode
                        counts[key] = (counts[key] ?: 0) + 1
                    }
                } else {
                    val cCode = colValue?.toString() ?: return@forEach
                    val key = rCode to cCode
                    counts[key] = (counts[key] ?: 0) + 1
                }
            }
        }
        return counts.map { (pair, count) ->
            MatrixSummaryItem(rowCode = pair.first, columnCode = pair.second, count = count)
        }
    }
}
