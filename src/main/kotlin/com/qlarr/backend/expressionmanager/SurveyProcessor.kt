package com.qlarr.backend.expressionmanager

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.qlarr.backend.api.surveyengine.NavigationJsonOutput
import com.qlarr.backend.api.surveyengine.ValidationJsonOutput
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.scriptengine.getNavigate
import com.qlarr.scriptengine.getValidate
import com.qlarr.surveyengine.ext.JsonExt
import com.qlarr.surveyengine.ext.engineScript
import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import com.qlarr.surveyengine.model.exposed.NavigationMode
import com.qlarr.surveyengine.model.exposed.SurveyMode
import com.qlarr.surveyengine.usecase.*

object SurveyProcessor {

    val scriptEngineNavigation = getNavigate(engineScript().script)
    val scriptEngineValidation = getValidate()

    private val scriptEngineValidate = object : ScriptEngineValidate {
        override fun validate(input: String): String {
            return scriptEngineValidation.validate(input)
        }
    }

    private val scriptEngineNavigate = object : ScriptEngineNavigate {
        override fun navigate(script: String): String {
            return scriptEngineNavigation.navigate(script)
        }

    }

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
        navigationMode: NavigationMode? = null,
        navigationIndex: NavigationIndex? = null,
        navigationDirection: NavigationDirection = NavigationDirection.Start,
        skipInvalid: Boolean,
        surveyMode: SurveyMode
    ): NavigationJsonOutput {
        val useCase = NavigationUseCaseWrapper.init(
            scriptEngineNavigate,
            values = values,
            processedSurvey = processedSurvey,
            lang = lang,
            navigationMode = navigationMode,
            navigationIndex = navigationIndex,
            navigationDirection = navigationDirection,
            skipInvalid = skipInvalid,
            surveyMode = surveyMode
        )
        val navigationJsonOutput = objectMapper.readValue(useCase.navigate(), jacksonTypeRef<NavigationJsonOutput>())
        return navigationJsonOutput
    }

    fun maskedValues(
        validationJsonOutput: ValidationJsonOutput,
        values: Map<String, Any>
    ): Map<String, Any> {
        val useCase = MaskedValuesUseCase(
            scriptEngineNavigate,
            validationJsonOutput.stringified(),
            objectMapper.writeValueAsString(values)
        )
        return useCase.navigate()
    }
}