package com.frankie.expressionmanager.validation

import com.frankie.expressionmanager.ext.flatten
import com.frankie.expressionmanager.ext.getDuplicates
import com.frankie.expressionmanager.ext.splitToComponentCodes
import com.frankie.expressionmanager.model.*

internal fun SurveyComponent.validateInstructions(): SurveyComponent {
    if (hasErrors()) {
        return this
    }
    var validatedComponent = duplicate()

    validatedComponent = validatedComponent
            .addInstructionDuplicateCodes()
            .validateRandomGroupInstruction()
            .validatePriorityGroupInstruction()
            .validateParentRelevanceInstruction()

    val newChildren = mutableListOf<SurveyComponent>()
    validatedComponent.children.forEach {
        val newChild = it.validateInstructions()
        newChildren.add(newChild)
    }
    validatedComponent = validatedComponent.duplicate(children = newChildren)

    return validatedComponent

}


internal fun SurveyComponent.validateReferences(sanitizedNestedComponents: List<ChildlessComponent>): SurveyComponent {
    if (hasErrors()) {
        return this
    }

    var validatedComponent = duplicate()
    validatedComponent = validatedComponent
            .validateReferenceInstructions(sanitizedNestedComponents)

    val newChildren = mutableListOf<SurveyComponent>()
    validatedComponent.children.forEach {
        val newChild = it.validateReferences(sanitizedNestedComponents)
        newChildren.add(newChild)
    }
    validatedComponent = validatedComponent.duplicate(children = newChildren)

    return validatedComponent

}

private fun SurveyComponent.validateReferenceInstructions(sanitizedNestedComponents: List<ChildlessComponent>): SurveyComponent {
    val newInstructions = instructionList.toMutableList()
    instructionList.forEachIndexed { index, instruction ->
        if (instruction is Instruction.Reference && instruction.noErrors()) {
            var newInstruction = instruction.copy()
            val newDependencies = instruction.references.toMutableList()
            instruction.references.forEach { dependency ->
                val codes = dependency.split(".")
                val componentCode = if (codes.isNotEmpty()) codes[0] else ""
                val referencedComponent = sanitizedNestedComponents.firstOrNull { it.code == componentCode }
                if (referencedComponent == null) {
                    newInstruction = newInstruction.addError(BindingErrors.InvalidReference(dependency, true))
                } else {
                    val reservedCode = if (codes.size >= 2) codes[1] else ""
                    if (!reservedCode.isReservedCode()) {
                        newInstruction = newInstruction.addError(BindingErrors.InvalidReference(dependency, false))
                    } else {
                        val referencedInstruction = referencedComponent
                                .instructionList
                                .filterIsInstance<Instruction.State>()
                                .firstOrNull { it.reservedCode == reservedCode.toReservedCode() }
                        if (referencedInstruction == null) {
                            newInstruction = newInstruction.addError(BindingErrors.InvalidReference(dependency, false))
                        }
                    }
                }
            }
            newInstructions[index] = newInstruction.copy(references = newDependencies)
        }
    }
    return duplicate(instructionList = newInstructions)
}


private fun SurveyComponent.addInstructionDuplicateCodes(): SurveyComponent {
    val newInstructions = instructionList.toMutableList()
    instructionList.forEachIndexed { index, instruction ->
        if (instructionList.count { it.code == instruction.code } > 1
                && instructionList.indexOfFirst { it.code == instruction.code } != index) {
            newInstructions[index] = instruction.addError(BindingErrors.DuplicateInstructionCode)
        }
    }
    return duplicate(instructionList = newInstructions)
}

private fun SurveyComponent.validateParentRelevanceInstruction(): SurveyComponent {
    val newInstructions = instructionList.toMutableList()
    instructionList.forEachIndexed { index, instruction ->
        if (instruction is Instruction.ParentRelevance && instruction.noErrors()) {
            instruction.children
                    .flatten().distinct()
                    .filter { code ->
                        children.all { it.code != code }
                    }.let {
                        if (it.isNotEmpty()) {
                            newInstructions[index] = instruction.addError(BindingErrors.InvalidChildReferences(it))
                        }
                    }
        }
    }
    return duplicate(instructionList = newInstructions)
}

fun List<SurveyComponent>.validateDuplicates(parentCode: String = ""): List<SurveyComponent> {
    if (isEmpty()) {
        return this
    }
    val returnList = toMutableList()

    val duplicateCodes = map { it.code }.getDuplicates()
    duplicateCodes.forEach { duplicateCode ->
        returnList.forEachIndexed { index, webComponent ->
            if (webComponent.code == duplicateCode
                    && indexOfFirst { it.code == webComponent.code } != index
            )
                returnList[index] = webComponent.addError(ComponentError.DUPLICATE_CODE)
        }
    }

    if (parentCode.isBlank()) {
        val newList = returnList.validateUniqueCodes()
        returnList.apply {
            clear()
            addAll(newList)
        }
    }
    return returnList
}


fun List<SurveyComponent>.validateEmptyParents(): List<SurveyComponent> {
    if (isEmpty()) {
        return this
    }
    val returnList = toMutableList()

    forEachIndexed { index, surveyComponent ->
        if (surveyComponent is Survey || surveyComponent is Group) {
            var newComponent = surveyComponent.duplicate()
            if (surveyComponent.children.count { it.noErrors() && (it as? Group)?.groupType != GroupType.END } == 0) {
                newComponent = newComponent.addError(ComponentError.EMPTY_PARENT)
            }
            returnList[index] = newComponent.duplicate(children = surveyComponent.children.validateEmptyParents())
        }
    }
    return returnList
}

fun List<SurveyComponent>.validateSpecialTypeGroups(): List<SurveyComponent> {
    if (isEmpty() || get(0) !is Survey) {
        return this
    }
    val survey = get(0) as Survey
    val children = survey.groups.toMutableList()
    children.forEachIndexed { index, group ->
        when (group.groupType) {
            GroupType.END -> if (index != children.size - 1) {
                children[index] = group.addError(ComponentError.MISPLACED_END_GROUP) as Group
            } else {
                children[index] = group.validateNoValueInEndGroup() as Group
            }

            GroupType.GROUP -> {
                // That should be fine
            }
        }
    }
    return if (survey.groups.none { it.groupType == GroupType.END && it.noErrors() }) {
        listOf(survey.copy(groups = children).addError(ComponentError.NO_END_GROUP))
    } else {
        listOf(survey.copy(groups = children))
    }
}

internal fun SurveyComponent.validateNoValueInEndGroup(): SurveyComponent {
    if (hasErrors()) {
        return this
    }
    val newInstructions = instructionList.toMutableList()
    if (this is Group) {
        instructionList.filterIsInstance<Instruction.SimpleState>().filter {
            it.reservedCode in listOf(ReservedCode.ConditionalRelevance)
        }.forEach {
            newInstructions[newInstructions.indexOf(it)] = it.addError(BindingErrors.InvalidInstructionInEndGroup)
        }
    }
    instructionList
            .filterNoErrors()
            .filterIsInstance<Instruction.SimpleState>()
            .firstOrNull {
                it.reservedCode == ReservedCode.Value
//                || it.reservedCode == ReservedCode.Validity
            }
            ?.let {
                newInstructions[newInstructions.indexOf(it)] = it.addError(BindingErrors.InvalidInstructionInEndGroup)
            }
    val newChildren = children.map { it.validateNoValueInEndGroup() }

    return duplicate(instructionList = newInstructions, children = newChildren)

}

private fun List<SurveyComponent>.validateUniqueCodes(): List<SurveyComponent> {
    if (isEmpty()) {
        return this
    }
    val returnList = toMutableList()

    val nestedComponents = uniqueNestedComponentsWithFullCode()
    val duplicateUniqueCodes = nestedComponents.mapNotNull { entry ->
        val list = entry.value
        if (list.size >= 2)
            list.takeLast(list.size - 1)
        else
            null
    }.flatten()

    returnList.forEachIndexed { index, webComponent ->
        var newComponent = webComponent.duplicate()
        duplicateUniqueCodes.forEach { duplicateCode ->
            newComponent = newComponent.addDuplicateErrorsToChildren(duplicateCode.splitToComponentCodes())
        }
        returnList[index] = newComponent
    }
    return returnList
}

private fun SurveyComponent.addDuplicateErrorsToChildren(duplicateCodes: List<String>): SurveyComponent {
    var returnElement = duplicate()
    if (duplicateCodes.size < 2 || duplicateCodes[0] != code) {
        return this
    } else if (duplicateCodes.size == 2) {
        val newChildren = returnElement.children.toMutableList()
        newChildren.forEachIndexed { index, surveyComponent ->
            if (surveyComponent.code == duplicateCodes[1]
                    && (newChildren.count { it.code == surveyComponent.code } == 1
                            || newChildren.indexOfFirst { it.code == surveyComponent.code } != index)
            ) {
                newChildren[index] = surveyComponent.addError(ComponentError.DUPLICATE_CODE)
            }
        }
        returnElement = returnElement.duplicate(children = newChildren)
    } else {
        val newChildren = returnElement.children.map {
            it.addDuplicateErrorsToChildren(duplicateCodes.takeLast(duplicateCodes.size - 1))
        }
        returnElement = returnElement.duplicate(children = newChildren)
    }
    return returnElement
}

private fun List<SurveyComponent>.uniqueNestedComponentsWithFullCode(): Map<String, List<String>> {

    val codes = mutableMapOf<String, List<String>>()
    forEach { element ->
        val map = element.uniqueNestedComponentsWithFullCode()
        map.keys.forEach { key ->
            val list = map.getValue(key)
            val currentList = codes[key] ?: listOf()
            val newList = mutableListOf<String>().apply {
                addAll(currentList)
                addAll(list)
            }
            codes[key] = newList
        }
    }
    return codes
}

private fun SurveyComponent.uniqueNestedComponentsWithFullCode(): Map<String, List<String>> {
    if (!this.hasUniqueCode()) {
        return mapOf()
    }
    val codes = mutableMapOf<String, List<String>>()
    codes[code] = listOf(code)
    children.forEach { element ->
        if (element.hasUniqueCode()) {
            val map = element.uniqueNestedComponentsWithFullCode()
            map.keys.forEach { key ->
                val list = map.getValue(key).map { code + it }
                val currentList = codes[key] ?: listOf()
                val newList = mutableListOf<String>().apply {
                    addAll(currentList)
                    addAll(list)
                }
                codes[key] = newList
            }
        }
    }
    return codes
}