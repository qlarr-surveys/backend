package com.frankie.backend.expressionmanager

import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.ext.JsonExt
import com.frankie.expressionmanager.ext.ScriptUtils
import com.frankie.expressionmanager.model.Dependency
import com.frankie.expressionmanager.model.NavigationUseCaseInput
import com.frankie.expressionmanager.model.SurveyMode
import com.frankie.expressionmanager.usecase.*
import com.frankie.scriptengine.ScriptEngineWrapper

object SurveyProcessor {

    val scriptEngineWrapper = ScriptEngineWrapper(ScriptUtils().engineScript)

    private val scriptEngine = object : ScriptEngine {
        override fun executeScript(method: String, script: String): String {
            return scriptEngineWrapper.executeScript(method, script)
        }
    }

    fun process(stateObj: ObjectNode): ValidationJsonOutput {
        val surveyNode = JsonExt.addChildren(stateObj["Survey"] as ObjectNode, "Survey", stateObj)
        val useCase = ValidationUseCaseWrapperImpl(scriptEngine, surveyNode.toString())
        return useCase.validate()
    }

    fun processSample(surveyNode: ObjectNode): ValidationJsonOutput {
        val useCase = ValidationUseCaseWrapperImpl(scriptEngine, surveyNode.toString())
        return useCase.validate()
    }

    fun navigate(
        validationJsonOutput: ValidationJsonOutput,
        useCaseInput: NavigationUseCaseInput,
        skipInvalid: Boolean,
        surveyMode:SurveyMode
    ): NavigationJsonOutput {
        val useCase = NavigationUseCaseWrapperImpl(
            scriptEngine,
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
        val useCase = MaskedValuesUseCase(scriptEngine, validationJsonOutput)
        return useCase.navigate(useCaseInput)
    }
}