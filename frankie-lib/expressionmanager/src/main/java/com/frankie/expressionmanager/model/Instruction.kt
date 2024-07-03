package com.frankie.expressionmanager.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.frankie.expressionmanager.ext.VALID_REFERENCE_INSTRUCTION_PATTERN
import com.frankie.expressionmanager.ext.VALID_REFERENCE_PREFIX
import com.frankie.expressionmanager.ext.flatten
import com.frankie.expressionmanager.model.Instruction.*
import com.frankie.expressionmanager.model.adapters.InstructionDeserializer
import com.frankie.expressionmanager.model.adapters.InstructionGson
import com.frankie.expressionmanager.model.adapters.InstructionSerializer
import com.google.gson.annotations.Expose
import com.google.gson.annotations.JsonAdapter

@JsonDeserialize(using = InstructionDeserializer::class)
@JsonSerialize(using = InstructionSerializer::class)
@JsonAdapter(InstructionGson::class)
sealed class Instruction(
    open val code: String,
    open val errors: List<BindingErrors>
) {
    abstract fun addError(error: BindingErrors): Instruction
    fun noErrors() = errors.isEmpty()
    abstract fun clearErrors(): Instruction


    @JsonAdapter(InstructionGson::class)
    data class Reference(
        override val code: String,
        val references: List<String>,
        val lang: String,
        override val errors: List<BindingErrors> = listOf()
    ) : Instruction(code, errors) {
        init {
            if (!code.matches(Regex(VALID_REFERENCE_INSTRUCTION_PATTERN))) {
                throw java.lang.IllegalArgumentException("Instruction code: $code must start with $VALID_REFERENCE_PREFIX")
            }
        }

        fun text(): String {
            val grouped = references.map { it.split(".") }.groupBy { it[0] }
            return grouped.keys.joinToString(separator = ", ", prefix = "{", postfix = "}", transform = { key ->
                val variables = grouped[key]!!
                "$key: " + variables.joinToString(separator = ", ", prefix = "{", postfix = "}", transform = { list ->
                    "${list[1]} : ${list[0]}.${list[1]}"
                })
            })
        }

        fun runnableInstruction() = RunnableInstruction(code, text(), ReturnType.FrankieMap, true, errors)

        override fun addError(error: BindingErrors) = copy(errors = errors.toMutableList().apply { add(error) })
        override fun clearErrors() = copy(errors = emptyList())
    }


    @JsonAdapter(InstructionGson::class)
    data class RandomGroups(
        val groups: List<RandomGroup> = listOf(),
        override val errors: List<BindingErrors> = listOf()
    ) : Instruction(RANDOM_GROUP, errors) {

        constructor(groups: List<List<String>>) : this(groups.map { RandomGroup(it) })

        override fun addError(error: BindingErrors) = copy(errors = errors.toMutableList().apply { add(error) })
        override fun clearErrors() = copy(errors = emptyList())
    }

    @JsonAdapter(InstructionGson::class)
    data class PriorityGroups(
        val priorities: List<PriorityGroup> = listOf(),
        override val errors: List<BindingErrors> = listOf()
    ) : Instruction(PRIORITY_GROUPS, errors) {
        override fun addError(error: BindingErrors) = copy(errors = errors.toMutableList().apply { add(error) })
        override fun clearErrors() = copy(errors = emptyList())
    }

    data class PriorityGroup(
        val weights: List<ChildPriority>,
        val limit: Int = weights.size - 1
    ) {
        constructor(codes: List<String>) : this(codes.map { ChildPriority(it) })
    }

    data class RandomGroup(
        val codes: List<String>,
        val randomOption: RandomOption = RandomOption.RANDOM
    )

    enum class RandomOption {
        RANDOM, ALPHA, FLIP
    }

    enum class FlipDirection {
        ASCENDING, DESCENDING
    }

    data class ChildPriority(val code: String, val weight: Float = 1F)

    @JsonAdapter(InstructionGson::class)
    data class ParentRelevance(
        val children: List<List<String>> = listOf(),
        override val errors: List<BindingErrors> = listOf()
    ) : Instruction(PARENT_RELEVANCE, errors) {
        override fun addError(error: BindingErrors) = copy(errors = errors.toMutableList().apply { add(error) })
        override fun clearErrors() = copy(errors = emptyList())
    }

    abstract class State(
        open val text: String,
        open val reservedCode: ReservedCode,
        open val returnType: ReturnType = reservedCode.defaultReturnType(),
        open val isActive: Boolean = reservedCode.defaultIsActive(),
        override val errors: List<BindingErrors> = listOf()
    ) : Instruction(reservedCode.code, errors) {
        fun validate() {
            if (reservedCode != ReservedCode.Value && returnType != reservedCode.defaultReturnType()) {
                throw IllegalArgumentException("invalid ReturnType:$returnType for $reservedCode")
            }
        }

        fun runnableInstruction() = RunnableInstruction(code, text, returnType, isActive, errors)

        abstract fun withValidatedInstruction(validatedInstruction: RunnableInstruction): State

    }


    @JsonAdapter(InstructionGson::class)
    data class SimpleState(
        override val text: String,
        override val reservedCode: ReservedCode,
        override val returnType: ReturnType = reservedCode.defaultReturnType(),
        override val isActive: Boolean = reservedCode.defaultIsActive(),
        override val errors: List<BindingErrors> = listOf()
    ) : State(reservedCode = reservedCode, text = text) {
        init {
            validate()
        }

        override fun withValidatedInstruction(validatedInstruction: RunnableInstruction) = copy(
            text = validatedInstruction.text,
            isActive = validatedInstruction.isActive,
            errors = validatedInstruction.errors
        )


        override fun addError(error: BindingErrors) = copy(errors = errors.toMutableList().apply { add(error) })
        override fun clearErrors() = copy(errors = emptyList())
        fun duplicate() = copy()
    }

    @JsonAdapter(InstructionGson::class)
    data class SkipInstruction(
        val skipToComponent: String = "",
        val toEnd: Boolean = false,
        val condition: String = "true",
        override val code: String = "skip_to_$skipToComponent",
        override val text: String = condition,
        override val reservedCode: ReservedCode = ReservedCode.Skip(code),
        override val isActive: Boolean = reservedCode.defaultIsActive(),
        override val errors: List<BindingErrors> = listOf()
    ) : State(reservedCode = reservedCode, text = condition, returnType = ReturnType.FrankieBoolean) {
        init {
            validate()
            if (!code.matches(Regex(SKIP_INSTRUCTION_PATTERN))) {
                throw java.lang.IllegalArgumentException("Invalid Skip Instruction code: $code")
            }
        }

        override fun withValidatedInstruction(validatedInstruction: RunnableInstruction) = copy(
            text = validatedInstruction.text,
            isActive = validatedInstruction.isActive,
            errors = validatedInstruction.errors
        )


        override fun addError(error: BindingErrors) = copy(errors = errors.toMutableList().apply { add(error) })
        override fun clearErrors() = copy(errors = emptyList())
        fun duplicate() = copy()
    }

    data class RunnableInstruction(
        val code: String,
        val text: String,
        val returnType: ReturnType,
        val isActive: Boolean,
        val errors: List<BindingErrors>
    )


    companion object {
        const val RANDOM_GROUP = "random_group"
        const val PRIORITY_GROUPS = "priority_groups"
        const val PARENT_RELEVANCE = "parent_relevance"
    }
}

fun List<Instruction>.filterNoErrors() = filter { instruction -> instruction.noErrors() }

data class ComponentInstruction(val componentCode: String, val instruction: RunnableInstruction)

internal fun SurveyComponent.randomGroups(options: List<RandomOption>): List<RandomGroup> {
    return instructionList.filterNoErrors().filterIsInstance<RandomGroups>()
        .firstOrNull()
        ?.groups
        ?.filter { options.contains(it.randomOption) }
        ?: listOf()
}


internal fun SurveyComponent.priorityGroups(): List<PriorityGroup> {
    return instructionList.filterNoErrors().filterIsInstance<PriorityGroups>().firstOrNull()?.priorities
        ?: listOf()
}

internal fun RandomGroups.codes(): Set<Set<String>> {
    return if (noErrors())
        groups.map { it.codes.toSet() }.toSet()
    else
        setOf()
}

internal fun PriorityGroups.codes(): Set<Set<String>> {
    return if (noErrors())
        priorities.map { priorityGroup -> priorityGroup.weights.map { it.code }.toSet() }.toSet()
    else
        setOf()
}

internal fun PriorityGroups.containingCode(code: String): PriorityGroup? {
    if (errors.isNotEmpty())
        return null
    priorities.forEach { priorityGroup ->
        if (priorityGroup.weights.any { it.code == code })
            return priorityGroup
    }
    return null
}

internal fun SurveyComponent.priorityGroupCodes(): List<String> {
    return priorityGroups().map { priorityGroup -> priorityGroup.weights.map { it.code } }.flatten()
}

