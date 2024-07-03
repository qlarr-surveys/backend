package com.frankie.expressionmanager.validation

import com.frankie.expressionmanager.ext.getDuplicates
import com.frankie.expressionmanager.model.BindingErrors
import com.frankie.expressionmanager.model.GroupType
import com.frankie.expressionmanager.model.Instruction.*
import com.frankie.expressionmanager.model.Survey
import com.frankie.expressionmanager.model.SurveyComponent


internal fun SurveyComponent.validateRandomGroupInstruction(): SurveyComponent {
    val newInstructions = instructionList.toMutableList()
    instructionList.forEachIndexed { index, instruction ->
        if (instruction is RandomGroups && instruction.noErrors()) {
            val duplicates = instruction.groups.map { it.codes }.flatten().getDuplicates()
            val itemsNotChildren = mutableListOf<String>()
            val invalidGroupItems = mutableListOf<String>()
            var newInstruction = instruction.copy()
            instruction.groups.removeDuplicates(duplicates).forEach { group ->
                group.codes.forEach { item ->
                    if (!hasChildWithCode(item)) {
                        itemsNotChildren.add(item)
                    } else if (isWelcomeOrEndGroup(item)) {
                        invalidGroupItems.add(item)
                    }
                }
            }
            if (duplicates.isNotEmpty()) {
                newInstruction = newInstruction.addError(BindingErrors.DuplicateRandomGroupItems(duplicates.toList()))
            }
            if (itemsNotChildren.isNotEmpty()) {
                // if all the children are removed... we remove this random group altogether
                val excludedGroups: List<RandomGroup> = instruction.groups
                    .filter { group -> group.codes.all { itemsNotChildren.contains(it) } }
                val newItemsNotChildren =
                    itemsNotChildren.filter { item -> excludedGroups.all { !it.codes.contains(item) } }
                newInstruction =
                    newInstruction.copy(groups = instruction.groups.filter { !excludedGroups.contains(it) })
                if (newItemsNotChildren.isNotEmpty()) {
                    newInstruction = newInstruction.addError(BindingErrors.RandomGroupItemNotChild(newItemsNotChildren))
                }
            }

            if (invalidGroupItems.isNotEmpty()) {
                newInstruction = newInstruction.addError(BindingErrors.InvalidRandomItem(invalidGroupItems))
            }

            newInstructions[index] = newInstruction
        }
    }
    return duplicate(instructionList = newInstructions)

}

internal fun SurveyComponent.validatePriorityGroupInstruction(): SurveyComponent {
    val newInstructions = instructionList.toMutableList()
    instructionList.forEachIndexed { index, instruction ->
        if (instruction is PriorityGroups && instruction.noErrors()) {
            val duplicates =
                instruction.priorities.map { priorityGroup -> priorityGroup.weights.map { it.code } }.flatten()
                    .getDuplicates()
            val itemsNotChildren = mutableListOf<String>()
            val invalidGroupItems = mutableListOf<String>()
            var newInstruction = instruction.copy()
            instruction.removeDuplicates(duplicates).forEach { group ->
                group.forEach { item ->
                    if (!hasChildWithCode(item.code)) {
                        itemsNotChildren.add(item.code)
                    } else if (isWelcomeOrEndGroup(item.code)) {
                        invalidGroupItems.add(item.code)
                    }
                }
            }
            if (instruction.priorities.any { it.limit >= it.weights.size || it.limit == 0 }) {
                newInstruction = newInstruction.addError(BindingErrors.PriorityLimitMismatch)
            }
            if (duplicates.isNotEmpty()) {
                newInstruction = newInstruction.addError(BindingErrors.DuplicatePriorityGroupItems(duplicates.toList()))
            }
            if (itemsNotChildren.isNotEmpty()) {
                // if all the children are removed... we remove this priority group altogether
                val excludedGroups: List<PriorityGroup> = instruction.priorities
                    .filter { group -> group.weights.all { itemsNotChildren.contains(it.code) } }
                val newItemsNotChildren = itemsNotChildren.filter {
                    !excludedGroups.map { priority -> priority.weights.map { it.code } }.flatten().contains(it)
                }
                newInstruction =
                    newInstruction.copy(priorities = instruction.priorities.filter { !excludedGroups.contains(it) })
                if (newItemsNotChildren.isNotEmpty()) {
                    newInstruction =
                        newInstruction.addError(BindingErrors.PriorityGroupItemNotChild(newItemsNotChildren))
                }
            }
            if (invalidGroupItems.isNotEmpty()) {
                newInstruction = newInstruction.addError(BindingErrors.InvalidPriorityItem(invalidGroupItems))
            }
            newInstructions[index] = newInstruction
        }
    }
    return duplicate(instructionList = newInstructions)

}

private fun SurveyComponent.hasChildWithCode(code: String): Boolean {
    return children.filter { it.noErrors() }.any { it.code == code }
}

private fun SurveyComponent.isWelcomeOrEndGroup(code: String): Boolean {
    return this is Survey && children.filter { it.noErrors() }.firstOrNull { it.code == code }?.let {
        it.groupType == GroupType.END
    } ?: false
}


private fun List<RandomGroup>.removeDuplicates(duplicates: Set<String>): List<RandomGroup> {
    val newGroups = mutableListOf<RandomGroup>()
    val encounters = mutableMapOf<String, Int>()
    forEach { group ->
        val newCodes = mutableListOf<String>()
        group.codes.forEach { item ->
            if (duplicates.contains(item) && (encounters[item] ?: 0) == 0) {
                encounters[item] = 1
                newCodes.add(item)

            } else if (!duplicates.contains(item)) {
                newCodes.add(item)
            }
        }
        newGroups.add(group.copy(codes = newCodes))
    }

    return newGroups
}

private fun PriorityGroups.removeDuplicates(duplicates: Set<String>): List<List<ChildPriority>> {
    val newGroups = mutableListOf<List<ChildPriority>>()
    val encounters = mutableMapOf<String, Int>()
    priorities.forEach { group ->
        val newGroup = mutableListOf<ChildPriority>()
        group.weights.forEach { item ->
            if (duplicates.contains(item.code) && (encounters[item.code] ?: 0) == 0) {
                encounters[item.code] = 1
                newGroup.add(item)

            } else if (!duplicates.contains(item.code)) {
                newGroup.add(item)
            }
        }
        newGroups.add(newGroup)
    }

    return newGroups
}

