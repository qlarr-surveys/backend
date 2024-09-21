package com.qlarr.backend.expressionmanager

import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.expressionmanager.ext.JsonExt
import com.qlarr.expressionmanager.ext.ScriptUtils
import com.qlarr.expressionmanager.model.Dependency
import com.qlarr.expressionmanager.model.NavigationUseCaseInput
import com.qlarr.expressionmanager.model.SurveyMode
import com.qlarr.expressionmanager.usecase.*
import com.qlarr.scriptengine.ScriptEngineNavigation
import com.qlarr.scriptengine.ScriptEngineValidation

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

    fun process(stateObj: ObjectNode): ValidationJsonOutput {
        val surveyNode = JsonExt.addChildren(stateObj["Survey"] as ObjectNode, "Survey", stateObj)
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