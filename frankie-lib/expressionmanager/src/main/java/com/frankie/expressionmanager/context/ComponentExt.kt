package com.frankie.expressionmanager.context

import com.frankie.expressionmanager.model.ReservedCode
import com.frankie.expressionmanager.model.ReturnType
import com.frankie.expressionmanager.ext.flatten
import com.frankie.expressionmanager.model.*

fun Survey.nestedComponents(): List<ChildlessComponent> {
    return mutableListOf<ChildlessComponent>().apply {
        add(toChildlessComponent(""))
        addAll(groups.nestedComponents(code))
    }
}

fun List<SurveyComponent>.nestedComponents(parentCode: String = ""): List<ChildlessComponent> {
    val returnList = mutableListOf<ChildlessComponent>()
    forEach { surveyComponent ->
        returnList.add(surveyComponent.withParentCode(parentCode).toChildlessComponent(parentCode))
        val newCode = surveyComponent.uniqueCode(parentCode)
        returnList.addAll(surveyComponent.children.nestedComponents(newCode))
    }
    return returnList
}

fun Survey.nestedComponentCodes(): List<String> {
    return mutableListOf<String>().apply {
        add(code)
        addAll(groups.nestedComponentCodes(code))
    }
}

fun List<SurveyComponent>.nestedComponentCodes(parentCode: String = ""): List<String> {
    val returnList = mutableListOf<String>()
    forEach { surveyComponent ->
        val code = surveyComponent.uniqueCode(parentCode)
        returnList.add(code)
        returnList.addAll(surveyComponent.children.nestedComponentCodes(code))
    }
    return returnList
}


fun List<SurveyComponent>.instructionsMap(parentCode: String = ""): LinkedHashMap<Dependency, Instruction.State> {
    val returnMap = linkedMapOf<Dependency, Instruction.State>()
    forEach { component ->
        val code = component.uniqueCode(parentCode)
        component.instructionList
            .filterIsInstance<Instruction.State>()
            .filter {
                it.reservedCode != ReservedCode.Validity
                        && it.reservedCode !is ReservedCode.ValidationRule
            }
            .sortedBy { it.reservedCode.executionOrder }.forEach {
                returnMap[Dependency(code, it.reservedCode)] = it
            }
        returnMap.putAll(component.children.instructionsMap(code))
        component.instructionList
            .filterIsInstance<Instruction.State>()
            .filter {
                it.reservedCode == ReservedCode.Validity
                        || it.reservedCode is ReservedCode.ValidationRule
            }
            .sortedBy { it.reservedCode.executionOrder }.forEach {
                returnMap[Dependency(code, it.reservedCode)] = it
            }
    }
    return returnMap
}

fun SurveyComponent.hasStateInstruction(reservedCode: ReservedCode) =
    instructionList.filterNoErrors().any { it.code == reservedCode.code }

fun SurveyComponent.hasActiveValidationRules() =
    instructionList.filterNoErrors().any {
        it is Instruction.State
                && it.reservedCode is ReservedCode.ValidationRule
                && it.isActive
    }

fun SurveyComponent.getActiveValidationRules() =
    instructionList.filterNoErrors()
        .filterIsInstance<Instruction.State>().filter {
            it.reservedCode is ReservedCode.ValidationRule
                    && it.isActive
        }

fun SurveyComponent.getStateInstruction(reservedCode: ReservedCode) =
    instructionList.filterNoErrors().firstOrNull { it.code == reservedCode.code } as? Instruction.State

fun SurveyComponent.hasStateInstruction(reservedCode: ReservedCode, isActive: Boolean) =
    instructionList.filterNoErrors()
        .any { it.code == reservedCode.code && (it as Instruction.State).isActive == isActive }


fun SurveyComponent.removeState(
    reservedCode: ReservedCode
): SurveyComponent {
    return duplicate(
        instructionList = instructionList
            .toMutableList()
            .apply { removeIf { it.code == reservedCode.code } })
}

fun SurveyComponent.removeStateIfNotActive(
    reservedCode: ReservedCode
): SurveyComponent {
    return duplicate(
        instructionList = instructionList
            .toMutableList()
            .apply { removeIf { it.code == reservedCode.code
                    && (it as? Instruction.SimpleState)?.isActive == false } })
}

fun SurveyComponent.insertOrOverrideState(
    reservedCode: ReservedCode,
    text: String,
    isActive: Boolean = false,
    returnType: ReturnType? = null,
): SurveyComponent {
    val instructionEqu = if (returnType == null)
        Instruction.SimpleState(text, reservedCode, isActive = isActive)
    else
        Instruction.SimpleState(text, reservedCode, isActive = isActive, returnType = returnType)

    return replaceOrAddInstruction(instructionEqu)
}

fun SurveyComponent.addErrorToInstruction(
    instruction: Instruction,
    error: BindingErrors
): SurveyComponent {
    val newInstructionList = instructionList.toMutableList()
    val newInstruction = instruction.addError(error)
    newInstructionList[instructionList.indexOfFirst { it.code == instruction.code }] = newInstruction
    return duplicate(instructionList = newInstructionList)

}

fun List<SurveyComponent>.indexableCodes(): List<String> {
    return mapNotNull {
            if (it.noErrors() && it.hasUniqueCode())
                mutableListOf(it.code)
                    .apply {
                        addAll(it.children.indexableCodes())
                    }
            else
                null
        }
        .flatten()
}