package com.frankie.backend.expressionmanager

import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.ext.JsonExt
import com.frankie.expressionmanager.ext.ScriptUtils
import com.frankie.expressionmanager.model.Dependency
import com.frankie.expressionmanager.model.NavigationUseCaseInput
import com.frankie.expressionmanager.model.SurveyMode
import com.frankie.expressionmanager.usecase.*
import com.frankie.scriptengine.ScriptEngineNavigation
import com.frankie.scriptengine.ScriptEngineValidation

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
            surveyMode:SurveyMode
    ): NavigationJsonOutput {
        val useCase = NavigationUseCaseWrapperImpl(
                scriptEngineNavigate,
                validationJsonOutput,
                useCaseInput,
                skipInvalid,
                surveyMode
        )
        return useCase.navigate()
    }

    fun maskedValues(
            validationJsonOutput: ValidationJsonOutput,
            useCaseInput: NavigationUseCaseInput
    ): Map<Dependency, Any> {
        val useCase = MaskedValuesUseCase(scriptEngineNavigate, validationJsonOutput)
        return useCase.navigate(useCaseInput)
    }
}