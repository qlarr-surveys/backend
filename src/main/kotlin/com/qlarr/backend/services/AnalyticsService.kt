package com.qlarr.backend.services

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.api.response.AnalyticsDto
import com.qlarr.backend.api.response.AnalyticsImage
import com.qlarr.backend.api.response.AnalyticsQuestion
import com.qlarr.backend.common.stripHtmlTags
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
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
    companion object {
        private val CHOICE_TYPES = setOf(
            "SCQ", "MCQ", "RANKING", "IMAGE_RANKING", "AUTOCOMPLETE",
            "ICON_SCQ", "ICON_MCQ", "IMAGE_SCQ", "IMAGE_MCQ"
        )
        private val MATRIX_TYPES = setOf("MATRIX_SCQ", "MATRIX_MCQ")
        private val RANKING_TYPES = setOf("RANKING", "IMAGE_RANKING")
        private val ICON_IMAGE_CHOICE_TYPES = setOf("ICON_SCQ", "ICON_MCQ", "IMAGE_SCQ", "IMAGE_MCQ")
        private val MULTI_FIELD_TYPES = setOf("MULTIPLE_TEXT", "MULTI_SHORT_TEXT")
        private val SINGLE_CHOICE_TYPES = setOf("SCQ", "AUTOCOMPLETE", "IMAGE_SCQ", "ICON_SCQ")
        private val MULTI_CHOICE_TYPES = setOf("MCQ", "IMAGE_MCQ", "ICON_MCQ")
        private val PRESENCE_ONLY_TYPES = setOf("SIGNATURE", "PHOTO_CAPTURE")
        private val CHILD_KEYS = listOf("children", "groups", "questions", "answers")
    }

    private data class AnalyticsContext(
        val labels: Map<String, String>,
        val schemaMap: Map<String, ResponseField>,
        val componentIndexList: List<ComponentIndex>,
        val questionTypes: Map<String, String>,
        val contentPaths: Map<String, List<String>>,
        val surveyId: UUID,
        val responses: List<SurveyResponseEntity>
    )

    fun getAnalytics(surveyId: UUID, maxResponses: Int): AnalyticsDto {
        val processed = designService.getLatestProcessedSurvey(surveyId)
        val survey = processed.survey
        val validationOutput = processed.validationJsonOutput

        // Get labels and component hierarchy
        val defaultLang = validationOutput.defaultSurveyLang().code
        val jarLabels = validationOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
        val supplementaryLabels = extractLabels(validationOutput.survey, defaultLang)
            .filterValues { it.isNotEmpty() }
            .stripHtmlTags()
        val labels = supplementaryLabels + jarLabels
        val schemaMap = validationOutput.schema
            .filter { it.columnName == ColumnName.VALUE }
            .associateBy { it.componentCode }

        // Extract question types and content paths from survey design
        val questionTypes = extractQuestionTypes(validationOutput.survey)
        val contentPaths = extractContentPaths(validationOutput.survey)

        // Fetch completed responses
        val pageable = Pageable.ofSize(maxResponses).withPage(0)
        val responses = responseRepository.findCompletedBySurveyId(surveyId, pageable).content

        val ctx = AnalyticsContext(labels, schemaMap, validationOutput.componentIndexList, questionTypes, contentPaths, surveyId, responses)

        // Build analytics questions
        val questionCodes = ctx.componentIndexList
            .map { it.code }
            .filter { it.isQuestionCode() }

        val questions = questionCodes.mapNotNull { buildAnalyticsQuestion(it, ctx) }

        return AnalyticsDto(
            surveyTitle = survey.name,
            totalResponses = responseRepository.completedSurveyCount(surveyId),
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

    private fun extractQuestionTypes(survey: ObjectNode): Map<String, String> {
        val types = mutableMapOf<String, String>()
        traverseSurveyTree(survey) { node, code, _ ->
            if (code != null && code.isQuestionCode()) {
                node.get("type")?.asText()?.let { types[code] = mapQuestionType(it) }
            }
        }
        return types
    }

    private fun extractContentPaths(survey: ObjectNode): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        traverseSurveyTree(survey) { node, code, parentQuestionCode ->
            if (code != null && code.startsWith("A") && parentQuestionCode != null) {
                resolveContentPaths(node)?.let { result[parentQuestionCode + code] = it }
            }
        }
        return result
    }

    private fun extractLabels(survey: ObjectNode, lang: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        traverseSurveyTree(survey) { node, code, parentQuestionCode ->
            if (code == null) return@traverseSurveyTree
            val fullCode = when {
                code.isQuestionCode() -> code
                code.startsWith("A") && parentQuestionCode != null -> parentQuestionCode + code
                else -> code
            }
            resolveNodeLabel(node, lang)?.let { result[fullCode] = it }
        }
        return result
    }

    // --- Node-level extraction helpers ---

    private fun resolveNodeLabel(node: ObjectNode, lang: String): String? {
        // Try content.{lang}.label
        val content = node.get("content") as? ObjectNode
        val langContent = content?.get(lang) as? ObjectNode
        val label = langContent?.get("label")?.asText()?.takeIf { it.isNotBlank() }
        if (label != null) return label

        val instructionList = node.get("instructionList") as? ArrayNode

        // Fallback: instructionList "format_label" instruction
        val formatLabelInst = instructionList?.firstOrNull { inst ->
            inst.get("code")?.asText() == "format_label"
                    && inst.get("lang")?.asText() == lang
        }
        val fromFormatLabel = formatLabelInst?.get("text")?.asText()?.takeIf { it.isNotBlank() }
            ?: formatLabelInst?.get("contentPath")?.let { cpNode ->
                (cpNode as? ArrayNode)?.firstOrNull()?.asText()?.takeIf { it.isNotBlank() }?.let { path ->
                    path.substringAfterLast("/").substringBeforeLast(".")
                }
            }
        if (fromFormatLabel != null) return fromFormatLabel

        // Fallback: any format_* instruction with contentPath
        val fromFormatAny = instructionList?.firstOrNull { inst ->
            inst.get("code")?.asText()?.startsWith("format_") == true
                    && (inst.get("contentPath") as? ArrayNode)?.size()?.let { it > 0 } == true
        }?.let { inst ->
            (inst.get("contentPath") as? ArrayNode)?.firstOrNull()?.asText()
                ?.takeIf { it.isNotBlank() }?.let { path ->
                    path.substringAfterLast("/").substringBeforeLast(".")
                }
        }
        if (fromFormatAny != null) return fromFormatAny

        // Fallback: resources node icon/image filename
        val resourcesNode = node.get("resources") as? ObjectNode
        val resourceFile = resourcesNode?.get("icon")?.asText() ?: resourcesNode?.get("image")?.asText()
        return resourceFile?.takeIf { it.isNotBlank() }?.let {
            it.substringAfterLast("/").substringBeforeLast(".")
        }
    }

    private fun resolveContentPaths(node: ObjectNode): List<String>? {
        // Check resources node first
        val resourcesNode = node.get("resources") as? ObjectNode
        if (resourcesNode != null) {
            val resourceFile = resourcesNode.get("icon")?.asText() ?: resourcesNode.get("image")?.asText()
            if (resourceFile != null) return listOf(resourceFile)
        }
        // Fallback: instructionList format_* with contentPath
        val instructionList = node.get("instructionList") as? ArrayNode
        return instructionList?.firstOrNull { inst ->
            inst.get("code")?.asText()?.startsWith("format_") == true
                    && (inst.get("contentPath") as? ArrayNode)?.size()?.let { it > 0 } == true
        }?.let { inst ->
            (inst.get("contentPath") as ArrayNode).map { it.asText() }
        }
    }

    // --- Label & value resolution helpers ---

    private fun resolveLabel(code: String, questionCode: String, labels: Map<String, String>): String {
        return labels[code] ?: labels[questionCode + code] ?: code
    }

    private fun resolveColumnValue(value: Any?, questionCode: String, labels: Map<String, String>): Any {
        return if (value is List<*>) {
            value.map { resolveLabel(it.toString(), questionCode, labels) }
        } else {
            resolveLabel(value.toString(), questionCode, labels)
        }
    }

    private fun isEmptyValue(value: Any?): Boolean = when (value) {
        null -> true
        is String -> value.isBlank()
        is List<*> -> value.isEmpty()
        is Map<*, *> -> value.isEmpty()
        else -> false
    }

    // --- Question type mapping ---

    private fun mapQuestionType(backendType: String): String {
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
            "ICON_SCQ" -> "ICON_SCQ"
            "ICON_MCQ" -> "ICON_MCQ"
            "FILE_UPLOAD", "FILE" -> "FILE_UPLOAD"
            "SIGNATURE" -> "SIGNATURE"
            "PHOTO_CAPTURE", "PHOTO" -> "PHOTO_CAPTURE"
            "BARCODE" -> "BARCODE"
            else -> backendType.uppercase()
        }
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
            answerCodes.map { ctx.labels[it] ?: it }
        } else null

        // Extract response values for this question
        val isMatrix = questionType in MATRIX_TYPES
        val isRanking = questionType in RANKING_TYPES
        val responseValues: List<Any?> = if (isRanking) {
            extractRankingFromAnswerValues(ctx, answerCodes)
        } else if (responseField != null) {
            extractResponses(questionType, responseField.toValueKey(), questionCode, ctx)
        } else if (isMatrix) {
            extractMatrixMultiFieldResponses(ctx, answerCodes, questionCode)
        } else {
            extractMultiFieldResponses(ctx, answerCodes)
        }

        val rows = if (isMatrix) {
            answerCodes
                .filter { it.removePrefix(questionCode).matches(Regex("^A\\d+$")) }
                .map { ctx.labels[it] ?: it.removePrefix(questionCode) }
        } else null

        val columns = if (isMatrix) {
            answerCodes
                .filter { it.removePrefix(questionCode).matches(Regex("^Ac\\d+$")) }
                .map { ctx.labels[it] ?: it.removePrefix(questionCode) }
        } else if (questionType in ICON_IMAGE_CHOICE_TYPES) {
            answerCodes.map { ctx.labels[it] ?: it.removePrefix(questionCode) }
        } else null

        return AnalyticsQuestion(
            id = questionCode,
            type = questionType,
            title = title,
            description = null,
            options = options,
            rows = rows,
            columns = columns,
            images = answerCodes.mapNotNull { answerCode ->
                ctx.contentPaths[answerCode]?.firstOrNull()?.let { resourceFile ->
                    AnalyticsImage(
                        id = answerCode,
                        label = ctx.labels[answerCode],
                        url = buildResourceUrl(ctx.surveyId, resourceFile)
                    )
                }
            }.ifEmpty { null },
            fields = if (questionType in MULTI_FIELD_TYPES) {
                answerCodes.map { ctx.labels[it] ?: it }
            } else null,
            responses = responseValues
        )
    }

    private fun buildResourceUrl(surveyId: UUID, fileName: String): String {
        return "/survey/$surveyId/resource/$fileName"
    }

    // --- Response extraction ---

    private fun extractResponses(
        type: String,
        valueKey: String,
        questionCode: String,
        ctx: AnalyticsContext
    ): List<Any?> {
        return ctx.responses.mapNotNull { response ->
            val value = response.values[valueKey] ?: return@mapNotNull null
            if (isEmptyValue(value)) return@mapNotNull null
            when {
                type in SINGLE_CHOICE_TYPES -> resolveLabel(value.toString(), questionCode, ctx.labels)
                type in MULTI_CHOICE_TYPES -> {
                    (value as? List<*>)?.map { resolveLabel(it.toString(), questionCode, ctx.labels) }
                }
                type in MATRIX_TYPES -> {
                    (value as? Map<*, *>)?.entries?.associate { (k, v) ->
                        resolveLabel(k.toString(), questionCode, ctx.labels) to
                                resolveColumnValue(v, questionCode, ctx.labels)
                    }
                }
                type in MULTI_FIELD_TYPES -> {
                    (value as? Map<*, *>)?.entries?.associate { (k, v) ->
                        (ctx.labels[k.toString()] ?: k.toString()) to v
                    } ?: value
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
            it.removePrefix(questionCode).matches(Regex("^A\\d+$"))
        }
        return ctx.responses.mapNotNull { response ->
            val fieldMap = rowCodes.mapNotNull mapField@{ answerCode ->
                val field = ctx.schemaMap[answerCode] ?: return@mapField null
                val value = response.values[field.toValueKey()] ?: return@mapField null
                if (isEmptyValue(value)) return@mapField null
                val rowLabel = ctx.labels[answerCode] ?: answerCode
                rowLabel to resolveColumnValue(value, questionCode, ctx.labels)
            }.toMap()
            fieldMap.ifEmpty { null }
        }
    }

    private fun extractRankingFromAnswerValues(
        ctx: AnalyticsContext,
        answerCodes: List<String>
    ): List<Any?> {
        return ctx.responses.mapNotNull { response ->
            val rankedItems = answerCodes.mapNotNull mapField@{ answerCode ->
                val field = ctx.schemaMap[answerCode] ?: return@mapField null
                val value = response.values[field.toValueKey()] ?: return@mapField null
                val rank = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: return@mapField null
                    else -> return@mapField null
                }
                rank to (ctx.labels[answerCode] ?: answerCode)
            }
            if (rankedItems.isEmpty()) null
            else rankedItems.sortedBy { it.first }.map { it.second }
        }
    }

    private fun extractMultiFieldResponses(
        ctx: AnalyticsContext,
        answerCodes: List<String>
    ): List<Any?> {
        return ctx.responses.mapNotNull { response ->
            val fieldMap = answerCodes.mapNotNull mapField@{ answerCode ->
                val field = ctx.schemaMap[answerCode] ?: return@mapField null
                val value = response.values[field.toValueKey()] ?: return@mapField null
                if (isEmptyValue(value)) return@mapField null
                (ctx.labels[answerCode] ?: answerCode) to value
            }.toMap()
            fieldMap.ifEmpty { null }
        }
    }
}

private fun String.isQuestionCode(): Boolean = startsWith("Q") && !contains("A")
