package com.qlarr.backend.expressionmanager

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.qlarr.backend.api.surveyengine.NavigationJsonOutput
import com.qlarr.backend.api.surveyengine.ValidationJsonOutput
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.surveyengine.ext.JsonExt
import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import com.qlarr.surveyengine.model.exposed.NavigationMode
import com.qlarr.surveyengine.model.exposed.SurveyMode
import com.qlarr.surveyengine.scriptengine.getNavigate
import com.qlarr.surveyengine.scriptengine.getValidate
import com.qlarr.surveyengine.usecase.NavigationUseCaseWrapper
import com.qlarr.surveyengine.usecase.ValidationUseCaseWrapper

object SurveyProcessor {

    private val scriptEngineNavigate = getNavigate()
    private val scriptEngineValidate = getValidate()


    fun process(stateObj: ObjectNode, savedDesign: ObjectNode): ValidationJsonOutput {
        val flatSurvey = objectMapper.readTree(JsonExt.flatObject(savedDesign.toString())) as ObjectNode
        stateObj.fieldNames().forEach {
            flatSurvey.set<JsonNode>(it, stateObj.get(it))
        }
        val surveyNode = JsonExt.addChildren(flatSurvey["Survey"].toString(), "Survey", flatSurvey.toString())
        val useCase = ValidationUseCaseWrapper.create(scriptEngineValidate, surveyNode)
        return objectMapper.readValue(useCase.validate(), jacksonTypeRef<ValidationJsonOutput>())
    }

    fun processSample(surveyNode: ObjectNode): ValidationJsonOutput {
        val useCase = ValidationUseCaseWrapper.create(scriptEngineValidate, surveyNode.toString())
        return objectMapper.readValue(useCase.validate(), jacksonTypeRef<ValidationJsonOutput>())
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