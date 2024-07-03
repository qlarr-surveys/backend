package com.frankie.expressionmanager.usecase

import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.context.execute.*
import com.frankie.expressionmanager.context.instructionsMap
import com.frankie.expressionmanager.context.nestedComponents
import com.frankie.expressionmanager.dependency.DependencyMapper
import com.frankie.expressionmanager.ext.*
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.RandomOption
import com.frankie.expressionmanager.model.ReservedCode.Order
import com.frankie.expressionmanager.model.ReservedCode.Priority
import com.frankie.expressionmanager.navigate.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.collections.flatten

interface NavigationUseCase {
    fun navigate(): NavigationOutput
    fun getNavigationScript(): String
    fun processNavigationResult(scriptResult: String): NavigationOutput
}

class NavigationUseCaseImp(
    private val scriptEngine: ScriptEngine,
    private val validationOutput: ValidationOutput,
    private val surveyJson: ObjectNode,
    stringValues: Map<String, Any> = mapOf(),
    private val navigationInfo: NavigationInfo = NavigationInfo(),
    private val navigationMode: NavigationMode,
    private val lang: String,
    private val skipInvalid: Boolean,
    private val surveyMode: SurveyMode
) : NavigationUseCase {
    private val values = stringValues.withDependencyKeys()
    private val startupRandomValues = mutableMapOf<Dependency, Int>()
    private val contextExecutor = ContextExecutor()
    private var survey = validationOutput.survey.sanitize()
        .replaceOrAddInstruction(Instruction.SimpleState(lang, ReservedCode.Lang)) as Survey
    private val dependencyMapper = DependencyMapper(validationOutput.impactMap)
    private val skipMap = validationOutput.skipMap
    private lateinit var contextRunner: ContextRunner


    override fun navigate(): NavigationOutput {
        val script = getNavigationScript()
        val scriptResult = scriptEngine.executeScript("navigate", script)
        return processNavigationResult(scriptResult)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getNavigationScript(): String {
        val alphaSorted = mutableMapOf<Dependency, Int>()
        if (navigationInfo.navigationDirection == NavigationDirection.Start) {
            startupRandomValues.putAll(survey.randomize(RandomOption.values().toList()) { getLabel(it) })
            startupRandomValues.putAll(survey.setPriorities())
        } else {
            alphaSorted.putAll(survey.randomize(listOf(RandomOption.ALPHA)) { getLabel(it) })
        }
        val labelsMap = survey.getLabels(surveyJson, "", lang, surveyJson.defaultLang(), dependencyMapper.impactMap)
        val valueBindings =
            values.toMutableMap().apply {
                putAll(startupRandomValues)
                putAll(alphaSorted)
                putAll(labelsMap)
                put(modeDependency, surveyMode.name.lowercase())
            }

        val instructionsMap = listOf(survey).instructionsMap()
        val orderPriorityValues = valueBindings.filterKeys {
            it.reservedCode in listOf(
                Order,
                Priority
            )
        } as Map<Dependency, Int>
        contextRunner = ContextRunner(
            instructionsMap,
            dependencyMapper.dependencyMap,
            orderPriorityValues,
            skipMap
        )
        // We want to assume that the survey is all shown, to get a good feeling of what is valid and what is not
        survey.componentsInCurrentNav(survey.allInOne()).forEach {
            valueBindings[Dependency(it, ReservedCode.InCurrentNavigation)] = true
        }
        val sequence = contextRunner.instructionsRefreshSequence()
        val referenceInstructions = survey.nestedComponents().map { childlessComponent ->
            childlessComponent.instructionList
                .filterIsInstance<Instruction.Reference>()
                .map { instruction ->
                    ComponentInstruction(childlessComponent.code, instruction.runnableInstruction())
                }
        }.flatten()
        return contextExecutor.getNavigationScript(instructionsMap, valueBindings, sequence, referenceInstructions)
    }

    override fun processNavigationResult(scriptResult: String): NavigationOutput {
        val navDependencies = contextRunner.navigationDependencies()
        val valuesMap = contextExecutor.processNavigationValues(scriptResult)
        val stateBindings = valuesMap.first
        val formatBindings = valuesMap.second
        val navigationBindings = stateBindings.filterBindings(navDependencies)
        survey = survey.sortByOrder(stateBindings.filterKeys { it.reservedCode == Order })
        val extraBindings = mutableMapOf<Dependency, Any>()
        val runtimeContextBuilder = RuntimeContextBuilder(
            stateBindings,
            dependencyMapper.impactMap.toMutableMap(),
            dependencyMapper.dependencyMap.toMutableMap()
        )
        val currentIndexValidity: Boolean = runtimeContextBuilder.addValidityInstruction(
            survey,
            navigationInfo.navigationIndex ?: survey.allInOne()
        )[Dependency(
            "Survey",
            ReservedCode.Validity
        )] as Boolean
        val newNavIndex = survey.navigate(navigationInfo, navigationMode, navigationBindings, skipInvalid, currentIndexValidity)

        extraBindings.putAll(runtimeContextBuilder.addShowErrorsInstruction(survey, !newNavIndex.showError))
        extraBindings.putAll(runtimeContextBuilder.addValidityInstruction(survey, newNavIndex))
        extraBindings.putAll(runtimeContextBuilder.addBeforeAfterNav(survey, newNavIndex))
        val contextComponent = survey.nestedComponents()
        val reducedSurvey = survey.reduce(newNavIndex)
        val bindings = stateBindings.toMutableMap().apply {
            putAll(extraBindings)
        }
        val dependenciesToSave = validationOutput.schema.filter {
            if (navigationInfo.navigationDirection == NavigationDirection.Start)
                it.columnName == ColumnName.ORDER || it.columnName == ColumnName.PRIORITY || values.keys.contains(
                    it.toDependency()
                )
            else
                values.keys.contains(it.toDependency())
        }.map { it.toDependency() }.toSet()

        val toSave = stateBindings.apply {
            putAll(startupRandomValues)
        }.filterBindings(dependenciesToSave)

        return NavigationOutput(
            reducedSurvey = reducedSurvey,
            orderedSurvey = survey,
            contextComponents = contextComponent,
            stateBindings = bindings,
            formatBindings = formatBindings,
            toSave = toSave,
            dependencyMapBundle = Pair(dependencyMapper.impactMap, dependencyMapper.dependencyMap),
            navigationIndex = newNavIndex,
            event = ResponseEvent.Navigation(
                direction = navigationInfo.navigationDirection,
                from = navigationInfo.navigationIndex?.stringIndex() ?: "",
                to = newNavIndex.stringIndex(),
                time = LocalDateTime.now(ZoneOffset.UTC)
            )
        )
    }

    private fun getLabel(qualifiedCode: String): String {
        val codes = qualifiedCode.splitToComponentCodes().toMutableList()
        if (codes[0].isQuestionCode()) {
            val groupCode = survey.groups.first { group -> group.questions.map { it.code }.contains(codes[0]) }.code
            codes.add(0, groupCode)
        }
        return surveyJson.getChild(codes).getLabel(lang, surveyJson.defaultLang())
    }
}

private fun Map<Dependency, Any>.filterBindings(dependencies: Set<Dependency>): Map<Dependency, Any> {
    return filterKeys { dependencies.contains(it) }
}


data class NavigationOutput(
    val orderedSurvey: Survey = Survey(),
    val reducedSurvey: Survey = Survey(),
    val contextComponents: List<ChildlessComponent> = listOf(),
    val stateBindings: Map<Dependency, Any> = mapOf(),
    val toSave: Map<Dependency, Any> = mapOf(),
    val dependencyMapBundle: DependencyMapBundle = DependencyMapBundle(mapOf(), mapOf()),
    val navigationIndex: NavigationIndex,
    val formatBindings: Map<Dependent, Any> = mapOf(),
    val event: ResponseEvent.Navigation
)