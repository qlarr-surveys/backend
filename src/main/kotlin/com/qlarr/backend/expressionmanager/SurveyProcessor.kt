package com.qlarr.backend.expressionmanager

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.surveyengine.ext.JsonExt
import com.qlarr.surveyengine.ext.ScriptUtils
import com.qlarr.surveyengine.model.Dependency
import com.qlarr.surveyengine.model.NavigationUseCaseInput
import com.qlarr.surveyengine.model.SurveyMode
import com.qlarr.surveyengine.usecase.*
import com.qlarr.scriptengine.ScriptEngineNavigation
import com.qlarr.scriptengine.ScriptEngineValidation
import com.qlarr.surveyengine.ext.flatten

object SurveyProcessor {

    val scriptEngineNavigation = ScriptEngineNavigation(ScriptUtils().engineScript)
    val scriptEngineValidation = ScriptEngineValidation()

    private val scriptEngineValidate = object : ScriptEngineValidate {
        override fun validate(input: List<ScriptValidationInput>): List<ScriptValidationOutput> {
            return scriptEngineValidation.validate(input)
        }
    }

    private val scriptEngineNavigate = object : ScriptEngineNavigate {
        override fun navigate(script: String): String {
            return scriptEngineNavigation.navigate(script)
        }

    }

    fun process(stateObj: ObjectNode, savedDesign: ObjectNode): ValidationJsonOutput {
        val flatSurvey = savedDesign.flatten()
        stateObj.fieldNames().forEach {
            flatSurvey.set<JsonNode>(it, stateObj.get(it))
        }
        val surveyNode = JsonExt.addChildren(flatSurvey["Survey"] as ObjectNode, "Survey", flatSurvey)
        val useCase = ValidationUseCaseWrapperImpl(scriptEngineValidate, surveyNode.toString())
        return useCase.validate()
    }

    fun processSample(surveyNode: ObjectNode): ValidationJsonOutput {
        val useCase = ValidationUseCaseWrapperImpl(scriptEngineValidate, surveyNode.toString())
        return useCase.validate()
    }

    fun navigate(
            validationJsonOutput: ValidationJsonOutput,
            useCaseInput: NavigationUseCaseInput,
            skipInvalid: Boolean,
            surveyMode: SurveyMode
    ): NavigationJsonOutput {
        val useCase = NavigationUseCaseWrapperImpl(
                validationJsonOutput,
                useCaseInput,
                skipInvalid,
                surveyMode
        )
        return useCase.navigate(scriptEngineNavigate)
    }

    fun maskedValues(
            validationJsonOutput: ValidationJsonOutput,
            useCaseInput: NavigationUseCaseInput
    ): Map<Dependency, Any> {
        val useCase = MaskedValuesUseCase(validationJsonOutput)
        return useCase.navigate(scriptEngineNavigate, useCaseInput)
    }
}