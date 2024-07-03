package com.frankie.expressionmanager.usecase

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.ext.copyReducedToJSON
import com.frankie.expressionmanager.model.*

interface NavigationUseCaseWrapper {
    fun navigate(): NavigationJsonOutput
    fun getNavigationScript(): String
    fun processNavigationResult(scriptResult: String): NavigationJsonOutput
}

class NavigationUseCaseWrapperImpl(
    scriptEngine: ScriptEngine,
    private val validationJsonOutput: ValidationJsonOutput,
    private val useCaseInput: NavigationUseCaseInput,
    skipInvalid: Boolean,
    surveyMode: SurveyMode
) : NavigationUseCaseWrapper {

    private val validationOutput: ValidationOutput = validationJsonOutput.toValidationOutput()

    private val useCase = NavigationUseCaseImp(
        scriptEngine,
        validationOutput,
        validationJsonOutput.survey,
        useCaseInput.values,
        useCaseInput.navigationInfo,
        validationJsonOutput.surveyNavigationData().navigationMode,
        useCaseInput.lang ?: validationJsonOutput.survey.defaultLang(),
        skipInvalid,
        surveyMode
    )

    override fun navigate(): NavigationJsonOutput {
        if (validationOutput.survey.hasErrors()) {
            throw SurveyDesignWithErrorException
        }
        val navigationOutput = useCase.navigate()
        return processNavigationOutput(navigationOutput)
    }

    override fun getNavigationScript() = useCase.getNavigationScript()

    override fun processNavigationResult(scriptResult: String): NavigationJsonOutput {
        val navigationOutput = useCase.processNavigationResult(scriptResult)
        return processNavigationOutput(navigationOutput)
    }

    private fun processNavigationOutput(navigationOutput: NavigationOutput): NavigationJsonOutput {
        val state = StateMachineWriter(navigationOutput.toScriptInput()).state()
        return navigationOutput.toNavigationJsonOutput(
            surveyJson = validationJsonOutput.survey, state = state,
            lang = useCaseInput.lang
        )
    }

}


data class ScriptInput(
    val contextComponents: List<ChildlessComponent>,
    val bindings: Map<Dependency, Any>,
    val dependencyMapBundle: DependencyMapBundle,
    val formatBindings: Map<Dependent, Any>,
)

data class NavigationJsonOutput(
    val survey: ObjectNode = JsonNodeFactory.instance.objectNode(),
    val state: ObjectNode = JsonNodeFactory.instance.objectNode(),
    val navigationIndex: NavigationIndex,
    val toSave: Map<String, Any> = mapOf(),
    val event: ResponseEvent.Navigation
)

private fun NavigationOutput.toScriptInput(): ScriptInput {
    return ScriptInput(
        contextComponents = contextComponents,
        bindings = stateBindings,
        dependencyMapBundle = dependencyMapBundle,
        formatBindings = formatBindings
    )
}

private fun NavigationOutput.toNavigationJsonOutput(
    state: ObjectNode, surveyJson: ObjectNode, lang: String?,
): NavigationJsonOutput {
    return NavigationJsonOutput(
        state = state,
        toSave = toSave.withStringKeys(),
        survey = surveyJson.copyReducedToJSON(orderedSurvey, reducedSurvey, lang, surveyJson.defaultLang()),
        navigationIndex = navigationIndex,
        event = event
    )
}

object SurveyDesignWithErrorException : Exception()