package com.frankie.expressionmanager.context.execute

import com.frankie.expressionmanager.ext.jsonValueToObject
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.model.*
import org.json.JSONObject

internal class ContextExecutor {
    fun processNavigationValues(navigationValues: String): Pair<MutableMap<Dependency, Any>, Map<Dependent, Any>> {
        val json = JSONObject(navigationValues)
        val stateValues = mutableMapOf<Dependency, Any>()
        val formatValues = mutableMapOf<Dependent, Any>()
        json.keys().forEach { componentName ->
            val vars = json.getJSONObject(componentName as String)
            vars.keys().forEach { key ->
                if ((key as String).isReservedCode()) {
                    stateValues[Dependency(componentName, key.toReservedCode())] = jsonValueToObject(vars[key])
                } else {
                    formatValues[Dependent(componentName, key)] = vars[key]
                }
            }
        }
        return Pair(stateValues, formatValues)
    }

    fun getNavigationScript(
        instructionsMap: LinkedHashMap<Dependency, Instruction.State>,
        valueBindings: Map<Dependency, Any>,
        sequence: List<Dependency>,
        formatInstructions: List<ComponentInstruction>
    ): String {
        val values = valueBindings
            .filterKeys { instructionsMap.containsKey(it) }
            .mapValues {
                TypedValue(
                    value = it.value,
                    returnType = instructionsMap[it.key]!!.returnType
                )
            }
        val navSequence = sequence.map {
            ComponentInstruction(it.componentCode, instructionsMap[it]!!.runnableInstruction())
        }
        val codes = values.keys.map { it.componentCode }.toMutableSet().apply {
            addAll(navSequence.map { it.componentCode })
            addAll(formatInstructions.map { it.componentCode })
        }
        val instructionNavigationInput = NavigationInstructionsInput(
            values = values.mapKeys { it.key.toValueKey() },
            sequence = navSequence,
            formatInstructions = formatInstructions,
            codes = codes.toList()
        )
        return jacksonKtMapper.writeValueAsString(instructionNavigationInput)
    }
}

data class NavigationInstructionsInput(
    val values: Map<String, TypedValue>,
    val sequence: List<ComponentInstruction>,
    val formatInstructions: List<ComponentInstruction>,
    val codes: List<String>
)