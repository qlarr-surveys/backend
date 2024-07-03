package com.frankie.expressionmanager.usecase

import com.frankie.expressionmanager.context.execute.ContextExecutor
import com.frankie.expressionmanager.context.execute.randomize
import com.frankie.expressionmanager.context.execute.sanitize
import com.frankie.expressionmanager.context.instructionsMap
import com.frankie.expressionmanager.context.nestedComponents
import com.frankie.expressionmanager.dependency.DependencyMapper
import com.frankie.expressionmanager.ext.*
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.navigate.allInOne
import com.frankie.expressionmanager.navigate.componentsInCurrentNav


@Suppress("unused")
class MaskedValuesUseCase(
    private val scriptEngine: ScriptEngine,
    validationJsonOutput: ValidationJsonOutput,
//    private val useCaseInput: NavigationUseCaseInput
) {
    private val validationOutput: ValidationOutput = validationJsonOutput.toValidationOutput()
    private val defaultLang = validationJsonOutput.survey.defaultLang()
    val lang =  defaultLang
    private var survey = validationOutput.survey.sanitize()
        .replaceOrAddInstruction(Instruction.SimpleState(lang, ReservedCode.Lang)) as Survey
    private val surveyJson = validationJsonOutput.survey
    private val contextExecutor = ContextExecutor()

    fun navigate(useCaseInput: NavigationUseCaseInput): Map<Dependency, Any> {
        val script = getNavigationScript(useCaseInput)
        val scriptResult = scriptEngine.executeScript("navigate", script)
        return processNavigationResult(scriptResult)
    }

    fun getNavigationScript(useCaseInput: NavigationUseCaseInput): String {
        val values = useCaseInput.values.withDependencyKeys()
        val startupRandomValues = mutableMapOf<Dependency, Int>()
        val dependencyMapper = DependencyMapper(validationOutput.impactMap)
        val alphaSorted = mutableMapOf<Dependency, Int>()

        alphaSorted.putAll(survey.randomize(listOf(Instruction.RandomOption.ALPHA)) { getLabel(it) })
        val labelsMap = survey.getLabels(surveyJson, "", lang, defaultLang, dependencyMapper.impactMap)
        val valueBindings =
            values.toMutableMap().apply {
                putAll(startupRandomValues)
                putAll(alphaSorted)
                putAll(labelsMap)
            }

        val instructionsMap = listOf(survey).instructionsMap()

        // We want to assume that the survey is all shown, to get a good feeling of what is valid and what is not
        survey.componentsInCurrentNav(survey.allInOne()).forEach {
            valueBindings[Dependency(it, ReservedCode.InCurrentNavigation)] = true
        }

        val sequence = instructionsMap.filterValues { !it.isActive }.keys.toMutableList()
            .apply {
                addAll(
                    instructionsMap.filterKeys { it.reservedCode == ReservedCode.MaskedValue }.keys.toList()
                )
            }

        val referenceInstructions = survey.nestedComponents().map { childlessComponent ->
            childlessComponent.instructionList
                .filterIsInstance<Instruction.Reference>()
                .map { instruction ->
                    ComponentInstruction(childlessComponent.code, instruction.runnableInstruction())
                }
        }.flatten()
        return contextExecutor.getNavigationScript(instructionsMap, valueBindings, sequence, referenceInstructions)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun processNavigationResult(scriptResult: String): Map<Dependency, Any> {
        val valuesMap = contextExecutor.processNavigationValues(scriptResult)
        return valuesMap.first.filterKeys { it.reservedCode == ReservedCode.MaskedValue }

    }

    private fun getLabel(qualifiedCode: String): String {
        val codes = qualifiedCode.splitToComponentCodes().toMutableList()
        if (codes[0].isQuestionCode()) {
            val groupCode = survey.groups.first { group -> group.questions.map { it.code }.contains(codes[0]) }.code
            codes.add(0, groupCode)
        }
        return surveyJson.getChild(codes).getLabel(lang, defaultLang)
    }

}


