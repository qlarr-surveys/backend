package com.frankie.expressionmanager.dependency

import com.frankie.expressionmanager.context.addErrorToInstruction
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.BindingErrors.ForwardDependency

internal class ForwardDependencyAnalyzer(
    private val components: MutableList<SurveyComponent>,
    private val dependencyMap: DependencyMap
) {
    private val componentIndices = components.componentIndices()

    fun validateForwardDependencies(): ForwardDependencyAnalyzer {
        components.forEachIndexed { index, surveyComponent ->
            components[index] = surveyComponent.validateForwardDependencies()
        }
        return this
    }

    private fun SurveyComponent.validateForwardDependencies(parentCode: String = ""): SurveyComponent {
        val uniqueCode = uniqueCode(parentCode)
        return if (this is Survey
            || this.hasErrors()
            || !dependencyMap.keys.map { it.componentCode }.contains(uniqueCode)
        ) {
            duplicate(
                children = children.toMutableList().map { child -> child.validateForwardDependencies(uniqueCode) })
        } else {
            val accessibleDependencies = componentIndices.accessibleDependencies(uniqueCode)
            var returnComponent = duplicate()
            instructionList.forEach { instruction ->
                val dependent = Dependent(uniqueCode, instruction.code)
                dependencyMap[dependent]?.forEach { dependency ->
                    //it is OK to depend on oneself
                    if (dependency.componentCode != uniqueCode
                        && dependency != langDependency
                        && dependency != modeDependency
                        && !accessibleDependencies.contains(dependency)
                    ) {
                        returnComponent =
                            returnComponent.addErrorToInstruction(instruction, ForwardDependency(dependency))
                    }
                }
            }
            returnComponent.duplicate(
                children = children.toMutableList().map { child -> child.validateForwardDependencies(uniqueCode) })
        }
    }

    fun validateSkipDestinations(): ForwardDependencyAnalyzer {
        val endGroupCode: String? = if (components.isNotEmpty()) {
            (components[0] as? Survey)
                ?.groups?.filter { it.noErrors() && it.groupType == GroupType.END }
                ?.map { it.code }?.firstOrNull()
        } else {
            null
        }
        components.forEachIndexed { index, surveyComponent ->
            components[index] = surveyComponent.validateSkipDestinations("", endGroupCode)
        }
        return this
    }

    private fun SurveyComponent.validateSkipDestinations(
        parentCode: String = "",
        endGroupCode: String?
    ): SurveyComponent {
        val uniqueCode = uniqueCode(parentCode)
        return if (this is Survey
            || this.hasErrors()
            || this.instructionList.none { it.noErrors() && it is Instruction.SkipInstruction }
        ) {
            duplicate(
                children = children.toMutableList()
                    .map { child -> child.validateSkipDestinations(uniqueCode, endGroupCode) })
        } else {
            val jumpDestinations = componentIndices.jumpDestinations(uniqueCode)
            var returnComponent = duplicate()
            instructionList
                .filterIsInstance<Instruction.SkipInstruction>()
                .filter { it.noErrors() }
                .forEach { instruction ->
                    if (!jumpDestinations.contains(instruction.skipToComponent)
                    ) {
                        returnComponent =
                            returnComponent.addErrorToInstruction(
                                instruction,
                                BindingErrors.InvalidSkipReference(instruction.skipToComponent)
                            )
                    } else if (instruction.skipToComponent == endGroupCode && instruction.toEnd) {
                        returnComponent =
                            returnComponent.addErrorToInstruction(
                                instruction,
                                BindingErrors.SkipToEndOfEndGroup
                            )
                    }
                }
            returnComponent.duplicate(
                children = children.toMutableList()
                    .map { child -> child.validateSkipDestinations(uniqueCode, endGroupCode) })
        }
    }


}


