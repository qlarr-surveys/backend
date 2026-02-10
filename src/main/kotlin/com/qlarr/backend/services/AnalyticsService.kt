package com.qlarr.backend.services

import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.api.response.AnalyticsDto
import com.qlarr.backend.api.response.AnalyticsImage
import com.qlarr.backend.api.response.AnalyticsQuestion
import com.qlarr.backend.common.stripHtmlTags
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.ext.splitToComponentCodes
import com.qlarr.surveyengine.model.ComponentIndex
import com.qlarr.surveyengine.model.exposed.ColumnName
import com.qlarr.surveyengine.model.exposed.ResponseField
import com.qlarr.surveyengine.model.exposed.ReturnType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class AnalyticsService(
    private val designService: DesignService,
    private val responseRepository: ResponseRepository
) {
    fun getAnalytics(surveyId: UUID, maxResponses: Int): AnalyticsDto {
        val processed = designService.getLatestProcessedSurvey(surveyId)
        val survey = processed.survey
        val validationOutput = processed.validationJsonOutput

        // Get labels and component hierarchy
        val labels = validationOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
        val componentIndexList = validationOutput.componentIndexList
        val schemaMap = validationOutput.schema
            .filter { it.columnName == ColumnName.VALUE }
            .associateBy { it.componentCode }

        // Extract question types from survey design
        val questionTypes = extractQuestionTypes(validationOutput.survey)

        // Fetch completed responses
        val pageable = Pageable.ofSize(maxResponses).withPage(0)
        val responsePage = responseRepository.findCompletedBySurveyId(surveyId, pageable)
        val responses = responsePage.content

        // Build analytics questions
        val questionCodes = componentIndexList
            .map { it.code }
            .filter { it.startsWith("Q") && !it.contains("A") }

        val questions = questionCodes.mapNotNull { questionCode ->
            buildAnalyticsQuestion(
                questionCode,
                questionTypes,
                labels,
                schemaMap,
                componentIndexList,
                responses
            )
        }

        return AnalyticsDto(
            surveyTitle = survey.name,
            totalResponses = responseRepository.completedSurveyCount(surveyId),
            questions = questions
        )
    }

    private fun extractQuestionTypes(survey: ObjectNode): Map<String, String> {
        val types = mutableMapOf<String, String>()
        traverseTree(survey, types)
        return types
    }

    private fun traverseTree(node: ObjectNode, types: MutableMap<String, String>) {
        val code = node.get("code")?.asText()
        val type = node.get("type")?.asText()

        if (code != null && type != null && code.startsWith("Q") && !code.contains("A")) {
            types[code] = mapQuestionType(type)
        }

        listOf("children", "groups", "questions", "answers").forEach { childKey ->
            node.get(childKey)?.forEach { child ->
                if (child is ObjectNode) {
                    traverseTree(child, types)
                }
            }
        }
    }

    private fun mapQuestionType(backendType: String): String {
        // Map from Qlarr backend question types to chart visualization types
        return when (backendType.uppercase()) {
            "SCQ", "SINGLECHOICEQUESTION" -> "SCQ"
            "MCQ", "MULTIPLECHOICEQUESTION" -> "MCQ"
            "NPS", "NETPROMOTERSCORE" -> "NPS"
            "RANKING" -> "RANKING"
            "NUMBER", "NUMERIC" -> "NUMBER"
            "DATE" -> "DATE"
            "TIME" -> "TIME"
            "DATETIME" -> "DATETIME"
            "MATRIX_SCQ", "SCQ_ARRAY", "SCQ_ICON_ARRAY" -> "MATRIX_SCQ"
            "MATRIX_MCQ", "MCQ_ARRAY" -> "MATRIX_MCQ"
            "TEXT" -> "TEXT"
            "SHORTTEXT" -> "SHORTTEXT"
            "PARAGRAPH" -> "PARAGRAPH"
            "LONGTEXT" -> "LONGTEXT"
            "EMAIL" -> "EMAIL"
            "MULTIPLE_TEXT" -> "MULTIPLE_TEXT"
            "MULTI_SHORT_TEXT", "MULTISHORTTEXT" -> "MULTI_SHORT_TEXT"
            "AUTOCOMPLETE" -> "AUTOCOMPLETE"
            "IMAGE_RANKING" -> "IMAGE_RANKING"
            "IMAGE_SCQ" -> "IMAGE_SCQ"
            "IMAGE_MCQ" -> "IMAGE_MCQ"
            "FILE_UPLOAD", "FILE" -> "FILE_UPLOAD"
            "SIGNATURE" -> "SIGNATURE"
            "PHOTO_CAPTURE", "PHOTO" -> "PHOTO_CAPTURE"
            "BARCODE" -> "BARCODE"
            else -> backendType.uppercase() // Fallback to original
        }
    }

    private fun buildAnalyticsQuestion(
        questionCode: String,
        questionTypes: Map<String, String>,
        labels: Map<String, String>,
        schemaMap: Map<String, ResponseField>,
        componentIndexList: List<ComponentIndex>,
        responses: List<com.qlarr.backend.persistence.entities.SurveyResponseEntity>
    ): AnalyticsQuestion? {
        val responseField = schemaMap[questionCode]
        val questionType = questionTypes[questionCode]
            ?: responseField?.let { inferTypeFromReturnType(it.dataType) }
            ?: return null
        val title = labels[questionCode] ?: questionCode

        // Get answer codes (children of this question)
        val componentIndex = componentIndexList.firstOrNull { it.code == questionCode }
        val answerCodes = componentIndex?.children ?: emptyList()

        // Build question metadata based on type
        val options = if (questionType in listOf("SCQ", "MCQ", "RANKING", "AUTOCOMPLETE")) {
            answerCodes.map { labels[it] ?: it }
        } else null

        // Extract response values for this question
        val isMatrix = questionType in listOf("MATRIX_SCQ", "MATRIX_MCQ")
        val responseValues = if (responseField != null) {
            val valueKey = responseField.toValueKey()
            extractResponses(questionType, valueKey, questionCode, responses, answerCodes, labels)
        } else if (isMatrix) {
            // For MATRIX types, values are stored per-row answer; rebuild as row→column label maps
            extractMatrixMultiFieldResponses(responses, answerCodes, questionCode, labels, schemaMap)
        } else {
            // Values stored at answer level (e.g., MULTIPLE_TEXT, MULTI_SHORT_TEXT)
            extractMultiFieldResponses(responses, answerCodes, labels, schemaMap)
        }

        val rows = if (isMatrix) {
            answerCodes
                .filter { it.removePrefix(questionCode).matches(Regex("^A\\d+$")) }
                .map { labels[it] ?: it }
        } else null
        val columns = if (isMatrix) {
            answerCodes
                .filter { it.removePrefix(questionCode).matches(Regex("^Ac\\d+$")) }
                .map { labels[it] ?: it }
        } else null

        return AnalyticsQuestion(
            id = questionCode,
            type = questionType,
            title = title,
            description = null,
            options = options,
            rows = rows,
            columns = columns,
            images = null, // TODO: Extract for IMAGE types
            fields = if (questionType in listOf("MULTIPLE_TEXT", "MULTI_SHORT_TEXT")) {
                answerCodes.map { labels[it] ?: it }
            } else null,
            responses = responseValues
        )
    }

    private fun inferTypeFromReturnType(dataType: ReturnType): String {
        return when (dataType) {
            is ReturnType.Enum -> "SCQ"
            is ReturnType.List -> "MCQ"
            ReturnType.Int -> "NUMBER"
            ReturnType.String -> "TEXT"
            ReturnType.Date -> "DATE"
            ReturnType.Map -> "MATRIX_SCQ"
            ReturnType.Boolean -> "SCQ"
            ReturnType.Double -> "NUMBER"
            ReturnType.File -> "FILE_UPLOAD"
        }
    }

    private fun extractResponses(
        type: String,
        valueKey: String,
        questionCode: String,
        responses: List<com.qlarr.backend.persistence.entities.SurveyResponseEntity>,
        answerCodes: List<String>,
        labels: Map<String, String>
    ): List<Any?> {
        return responses.mapNotNull { response ->
            val value = response.values[valueKey] ?: return@mapNotNull null
            if (value is String && value.isBlank()) return@mapNotNull null
            if (value is List<*> && value.isEmpty()) return@mapNotNull null
            if (value is Map<*, *> && value.isEmpty()) return@mapNotNull null
            when (type) {
                "SCQ", "AUTOCOMPLETE", "IMAGE_SCQ" -> {
                    // value is a string answer code like "Q1A1", map to label
                    labels[value.toString()] ?: value.toString()
                }
                "MCQ", "IMAGE_MCQ" -> {
                    // value is a list of answer codes, map each to label
                    (value as? List<*>)?.map { labels[it.toString()] ?: it.toString() }
                }
                "RANKING", "IMAGE_RANKING" -> {
                    // value is an ordered list of answer codes
                    (value as? List<*>)?.map { labels[it.toString()] ?: it.toString() }
                }
                "NPS", "NUMBER" -> {
                    // Numeric value
                    value
                }
                "TEXT", "SHORTTEXT", "EMAIL", "PARAGRAPH", "LONGTEXT", "BARCODE", "TIME" -> {
                    // String value
                    value
                }
                "DATE", "DATETIME" -> {
                    // Date string
                    value
                }
                "MATRIX_SCQ", "MATRIX_MCQ" -> {
                    // value is a map {shortRowCode: shortColumnCode}
                    // Short codes like "Ac1" need questionCode prefix for label lookup
                    (value as? Map<*, *>)?.entries?.associate { (k, v) ->
                        val rowKey = k.toString()
                        val rowLabel = labels[questionCode + rowKey] ?: labels[rowKey] ?: rowKey
                        val colValue = if (v is List<*>) {
                            v.map {
                                val colKey = it.toString()
                                labels[questionCode + colKey] ?: labels[colKey] ?: colKey
                            }
                        } else {
                            val colKey = v.toString()
                            labels[questionCode + colKey] ?: labels[colKey] ?: colKey
                        }
                        rowLabel to colValue
                    }
                }
                "MULTIPLE_TEXT", "MULTI_SHORT_TEXT" -> {
                    // value is a map {answerCode: textValue}, remap keys to labels
                    (value as? Map<*, *>)?.entries?.associate { (k, v) ->
                        (labels[k.toString()] ?: k.toString()) to v
                    } ?: value
                }
                "SIGNATURE", "PHOTO_CAPTURE" -> {
                    // Extract completion status
                    when (value) {
                        is Map<*, *> -> value["signed"] ?: value["captured"] ?: false
                        else -> value
                    }
                }
                "FILE_UPLOAD" -> {
                    // Extract file metadata
                    value
                }
                else -> value
            }
        }
    }

    private fun extractMatrixMultiFieldResponses(
        responses: List<com.qlarr.backend.persistence.entities.SurveyResponseEntity>,
        answerCodes: List<String>,
        questionCode: String,
        labels: Map<String, String>,
        schemaMap: Map<String, ResponseField>
    ): List<Any?> {
        // Only process row answer codes (A followed by digits, not Ac)
        val rowCodes = answerCodes.filter {
            it.removePrefix(questionCode).matches(Regex("^A\\d+$"))
        }
        return responses.mapNotNull { response ->
            val fieldMap = rowCodes.mapNotNull mapField@{ answerCode ->
                val field = schemaMap[answerCode] ?: return@mapField null
                val valueKey = field.toValueKey()
                val value = response.values[valueKey] ?: return@mapField null
                if (value is String && value.isBlank()) return@mapField null
                val rowLabel = labels[answerCode] ?: answerCode
                val colValue = if (value is List<*>) {
                    value.map {
                        val colKey = it.toString()
                        labels[questionCode + colKey] ?: labels[colKey] ?: colKey
                    }
                } else {
                    val colKey = value.toString()
                    labels[questionCode + colKey] ?: labels[colKey] ?: colKey
                }
                rowLabel to colValue
            }.toMap()
            fieldMap.ifEmpty { null }
        }
    }

    private fun extractMultiFieldResponses(
        responses: List<com.qlarr.backend.persistence.entities.SurveyResponseEntity>,
        answerCodes: List<String>,
        labels: Map<String, String>,
        schemaMap: Map<String, ResponseField>
    ): List<Any?> {
        return responses.mapNotNull { response ->
            val fieldMap = answerCodes.mapNotNull mapField@{ answerCode ->
                val field = schemaMap[answerCode] ?: return@mapField null
                val valueKey = field.toValueKey()
                val value = response.values[valueKey] ?: return@mapField null
                if (value is String && value.isBlank()) return@mapField null
                (labels[answerCode] ?: answerCode) to value
            }.toMap()
            fieldMap.ifEmpty { null }
        }
    }
}
