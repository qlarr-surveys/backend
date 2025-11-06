package com.qlarr.backend.expressionmanager

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.qlarr.backend.api.surveyengine.NavigationJsonOutput
import com.qlarr.backend.api.surveyengine.ValidationJsonOutput
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.backend.exceptions.DuplicateToCodeException
import com.qlarr.backend.exceptions.FromCodeNotAvailableException
import com.qlarr.backend.exceptions.IdenticalFromToCodesException
import com.qlarr.backend.exceptions.InvalidCodeChangeException
import com.qlarr.surveyengine.ext.*
import com.qlarr.surveyengine.model.ReservedCode
import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import com.qlarr.surveyengine.model.exposed.NavigationMode
import com.qlarr.surveyengine.model.exposed.SurveyMode
import com.qlarr.surveyengine.model.parents
import com.qlarr.surveyengine.model.toImpactMap
import com.qlarr.surveyengine.scriptengine.getNavigate
import com.qlarr.surveyengine.usecase.ChangeCodeUseCaseWrapper
import com.qlarr.surveyengine.usecase.NavigationUseCaseWrapper
import com.qlarr.surveyengine.usecase.ValidationUseCaseWrapper

object SurveyProcessor {

    private val scriptEngineNavigate = getNavigate()

    fun process(stateObj: ObjectNode, savedDesign: ObjectNode): ValidationJsonOutput {
        val flatSurvey = objectMapper.readTree(JsonExt.flatObject(savedDesign.toString())) as ObjectNode
        stateObj.fieldNames().forEach {
            flatSurvey.set<JsonNode>(it, stateObj.get(it))
        }
        val surveyNode = JsonExt.addChildren(flatSurvey["Survey"].toString(), "Survey", flatSurvey.toString())
        val useCase = ValidationUseCaseWrapper.create(surveyNode)
        return objectMapper.readValue(useCase.validate(), jacksonTypeRef<ValidationJsonOutput>())
    }

    fun processSample(surveyNode: ObjectNode): ValidationJsonOutput {
        val useCase = ValidationUseCaseWrapper.create(surveyNode.toString())
        return objectMapper.readValue(useCase.validate(), jacksonTypeRef<ValidationJsonOutput>())
    }

    private fun wrongType(from: String, to: String): Boolean {
        val fromSplit = from.splitToComponentCodes()
        val toSplit = to.splitToComponentCodes()

        return (from.isGroupCode() && !to.isGroupCode())
                || (to.isGroupCode() && !from.isGroupCode())
                || (from.isQuestionCode() && !to.isQuestionCode())
                || (to.isQuestionCode() && !from.isQuestionCode())
                || (fromSplit.size != toSplit.size)
                || fromSplit.size > 1 && from.take(fromSplit.size - 1) != to.take(fromSplit.size - 1)
                || (fromSplit.size > 1 && (!fromSplit.last().isAnswerCode() || !toSplit.last().isAnswerCode()))

    }

    fun changeCode(surveyDesign: String, from: String, to: String): ValidationJsonOutput {
        val source: ValidationJsonOutput = objectMapper.readValue(surveyDesign, jacksonTypeRef<ValidationJsonOutput>())
        if (from == to) {
            throw IdenticalFromToCodesException()
        } else if (source.componentIndexList.all { it.code != from }) {
            throw FromCodeNotAvailableException()
        } else if (source.componentIndexList.any { it.code == to }) {
            throw DuplicateToCodeException()
        } else if (wrongType(from, to)) {
            throw InvalidCodeChangeException()
        }
        val useCase = ChangeCodeUseCaseWrapper.create(surveyDesign)
        val result = objectMapper.readValue(useCase.changeCode(from, to), jacksonTypeRef<ValidationJsonOutput>())
        val impactMap = result.impactMap.toImpactMap()
        impactMap.keys.filter { it.componentCode.contains(to) }.map {
            impactMap[it]!!.filter { it.instructionCode == ReservedCode.ConditionalRelevance.code }
        }.flatten().forEach {
            val path =
                result.componentIndexList.parents(it.componentCode) + it.componentCode.splitToComponentCodes().last()
            result.survey.changeRelevanceObject(path, from, to)
        }
        if (from.isGroupCode() || from.isQuestionCode()) {
            result.componentIndexList.filter { it.hasSkip() }.forEach { item ->
                val impactedElementCode = item.code
                val path =
                    result.componentIndexList.parents(impactedElementCode) + impactedElementCode.splitToComponentCodes()
                        .last()
                result.survey.changeSkipLogicObject(path, from, to)
            }
        }

        return result
    }


    fun navigate(
        values: String = "{}",
        processedSurvey: String,
        lang: String? = null,
        navigationMode: NavigationMode,
        navigationIndex: NavigationIndex? = null,
        navigationDirection: NavigationDirection = NavigationDirection.Start,
        skipInvalid: Boolean,
        surveyMode: SurveyMode
    ): NavigationJsonOutput {
        val useCase = NavigationUseCaseWrapper.init(
            values = values,
            processedSurvey = processedSurvey,
            lang = lang,
            navigationMode = navigationMode,
            navigationIndex = navigationIndex,
            navigationDirection = navigationDirection,
            skipInvalid = skipInvalid,
            surveyMode = surveyMode
        )
        val navigationJsonOutput =
            objectMapper.readValue(useCase.navigate(scriptEngineNavigate), jacksonTypeRef<NavigationJsonOutput>())
        return navigationJsonOutput
    }


    fun maskedValues(values: Map<String, Any>): Map<String, Any> {
        return buildMap {
            values.filterKeys {
                it.endsWith(".value")
            }.forEach { (key, _) ->
                val prefix = key.substringBeforeLast(".value")
                val maskedKey = "$prefix.masked_value"
                values[maskedKey]?.let { maskedValue ->
                    put(maskedKey, maskedValue)
                }
            }
        }
    }
}
