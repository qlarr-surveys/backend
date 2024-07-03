package com.frankie.expressionmanager.usecase

import com.frankie.expressionmanager.model.ResponseField
import com.frankie.expressionmanager.context.build.ContextBuilder
import com.frankie.expressionmanager.context.build.NotSkippedInstructionManifesto
import com.frankie.expressionmanager.context.build.getSchema
import com.frankie.expressionmanager.context.build.runtimeScript
import com.frankie.expressionmanager.dependency.DependencyMapper
import com.frankie.expressionmanager.model.ComponentIndex
import com.frankie.expressionmanager.model.StringImpactMap
import com.frankie.expressionmanager.model.Survey
import com.frankie.expressionmanager.model.toStringImpactMap

interface ValidationUseCase {
    fun validate(validateSpecialTypeGroups: Boolean = true): ValidationOutput
}

class ValidationUseCaseImpl(scriptEngine: ScriptEngine, survey: Survey) : ValidationUseCase {
    private val contextManager = ContextBuilder(listOf(survey).toMutableList(), scriptEngine)
    override fun validate(validateSpecialTypeGroups: Boolean): ValidationOutput {

        contextManager.validate(validateSpecialTypeGroups)
        val sanitisedComponents = contextManager.sanitizedNestedComponents
        val dependencyMapper = DependencyMapper(sanitisedComponents)
        return ValidationOutput(
            contextManager.components[0] as Survey,
            dependencyMapper.impactMap.toStringImpactMap(),
            contextManager.components.getSchema(),
            script = sanitisedComponents.runtimeScript(dependencyMapper.dependencyMap),
            componentIndexList = contextManager.componentIndexList,
            skipMap = contextManager.skipMap
        )
    }

}

data class ValidationOutput(
    val survey: Survey = Survey(),
    val impactMap: StringImpactMap = mapOf(),
    val schema: List<ResponseField> = listOf(),
    val script: String,
    val componentIndexList: List<ComponentIndex>,
    val skipMap: Map<String, List<NotSkippedInstructionManifesto>>
)