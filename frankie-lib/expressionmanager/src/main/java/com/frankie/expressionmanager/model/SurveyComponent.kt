package com.frankie.expressionmanager.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.frankie.expressionmanager.model.adapters.SurveyComponentGson
import com.google.gson.annotations.JsonAdapter


data class ChildlessComponent(
    val code: String,
    val parentCode: String,
    val surveyElementType: SurveyElementType,
    val instructionList: List<Instruction> = listOf()
) {
    fun getDependencies(): List<Dependency> {
        return instructionList
            .filterIsInstance<Instruction.State>()
            .map { Dependency(code, it.reservedCode) }
    }
}


@JsonAdapter(SurveyComponentGson::class)
sealed class SurveyComponent(
    open val code: String,
    open val instructionList: List<Instruction>,
    open val errors: List<ComponentError>
) {
    abstract fun clearErrors(): SurveyComponent
    fun hasUniqueCode(): Boolean = elementType.hasUniqueCode()
    fun uniqueCode(parentCode: String) = if (elementType.hasUniqueCode()) code else parentCode + code
    abstract val children: List<SurveyComponent>
    abstract val elementType: SurveyElementType
    fun hasErrors() = errors.isNotEmpty()
    fun noErrors() = !hasErrors()
    abstract fun duplicate(
        instructionList: List<Instruction> = this.instructionList,
        children: List<SurveyComponent> = this.children,
        errors: List<ComponentError> = this.errors
    ): SurveyComponent

    fun toChildlessComponent(parentCode: String) = ChildlessComponent(
        code = code,
        parentCode = parentCode,
        surveyElementType = elementType,
        instructionList = instructionList
    )

    fun accessibleDependencies(): List<ReservedCode> {
        return this.instructionList
            .filter { it.noErrors() }
            .filterIsInstance<Instruction.State>()
            .map { it.reservedCode }
            .filter { it.isAccessible }
    }

    fun withValidatedInstruction(validatedInstruction: Instruction.RunnableInstruction): SurveyComponent {
        val newList = instructionList.toMutableList()
        val index = instructionList.indexOfFirst { it.code == validatedInstruction.code }
        val newInstruction = (newList[index] as Instruction.State)
            .withValidatedInstruction(validatedInstruction)
        newList[index] = newInstruction
        return duplicate(instructionList = newList)
    }

    fun replaceOrAddInstruction(newInstruction: Instruction): SurveyComponent {
        val newList = instructionList.toMutableList()
        val index = instructionList.indexOfFirst { it.code == newInstruction.code }
        if (index != -1) {
            newList[index] = newInstruction
        } else {
            newList.add(newInstruction)
        }

        return duplicate(instructionList = newList)
    }

    fun addError(error: ComponentError) = duplicate(errors = errors.toMutableList().apply { add(error) })
    abstract fun withParentCode(parentCode: String): SurveyComponent

}

internal fun SurveyComponent.wrapToSurvey(): Survey {
    return when (this) {
        is Survey -> this
        is Group -> Survey(groups = listOf(this))
        is Question -> Group("G1", questions = listOf(this)).wrapToSurvey()
        is Answer -> throw IllegalStateException("We are going too far here!!")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAdapter(SurveyComponentGson::class)
data class Survey(
    override val instructionList: List<Instruction> = listOf(),
    val groups: List<Group> = listOf(),
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent("Survey", instructionList, errors) {

    @JsonIgnore
    override val elementType: SurveyElementType = SurveyElementType.SURVEY

    @JsonIgnore
    override val children = groups


    init {
        if (!this.code.matches(Regex(elementType.codeRegex))) {
            throw IllegalStateException("Wrong code: $code for $elementType:${elementType.codeRegex}")
        }
    }

    override fun duplicate(
        instructionList: List<Instruction>,
        children: List<SurveyComponent>,
        errors: List<ComponentError>
    ) =
        copy(instructionList = instructionList, errors = errors, groups = children.filterIsInstance<Group>())

    override fun withParentCode(parentCode: String) = duplicate()

    override fun clearErrors(): SurveyComponent {
        return copy(errors = emptyList(),
            instructionList = instructionList.map { it.clearErrors() },
            groups = groups.map { it.clearErrors() as Group }
        )
    }
}

enum class GroupType {
    END, GROUP
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAdapter(SurveyComponentGson::class)
data class Group(
    override val code: String,
    override val instructionList: List<Instruction> = listOf(),
    val questions: List<Question> = listOf(),
    val groupType: GroupType = GroupType.GROUP,
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent(code, instructionList, errors) {

    @JsonIgnore
    override val elementType: SurveyElementType = SurveyElementType.GROUP

    @JsonIgnore
    override val children = questions


    init {
        if (!this.code.matches(Regex(elementType.codeRegex))) {
            throw IllegalStateException("Wrong code: $code for $elementType:${elementType.codeRegex}")
        }
    }

    override fun duplicate(
        instructionList: List<Instruction>,
        children: List<SurveyComponent>,
        errors: List<ComponentError>
    ) =
        copy(instructionList = instructionList, errors = errors, questions = children.filterIsInstance<Question>())

    override fun withParentCode(parentCode: String) = duplicate()

    override fun clearErrors(): SurveyComponent {
        return copy(errors = emptyList(),
            instructionList = instructionList.map { it.clearErrors() },
            questions = questions.map { it.clearErrors() as Question }
        )
    }

    @JsonIgnore
    fun isNotEndGroup() = groupType != GroupType.END
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAdapter(SurveyComponentGson::class)
data class Question(
    override val code: String,
    override val instructionList: List<Instruction> = listOf(),
    val answers: List<Answer> = listOf(),
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent(code, instructionList, errors) {

    @JsonIgnore
    override val elementType: SurveyElementType = SurveyElementType.QUESTION

    @JsonIgnore
    override val children = answers


    init {
        if (!this.code.matches(Regex(elementType.codeRegex))) {
            throw IllegalStateException("Wrong code: $code for $elementType:${elementType.codeRegex}")
        }
    }

    override fun duplicate(
        instructionList: List<Instruction>,
        children: List<SurveyComponent>,
        errors: List<ComponentError>
    ) =
        copy(
            instructionList = instructionList, errors = errors,
            answers = children.filterIsInstance<Answer>()
        )

    override fun withParentCode(parentCode: String) = duplicate()

    override fun clearErrors(): SurveyComponent {
        return copy(errors = emptyList(),
            instructionList = instructionList.map { it.clearErrors() },
            answers = answers.map { it.clearErrors() as Answer }
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAdapter(SurveyComponentGson::class)
data class Answer(
    override val code: String,
    override val instructionList: List<Instruction> = listOf(),
    val answers: List<Answer> = listOf(),
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent(code, instructionList, errors) {

    @JsonIgnore
    override val elementType: SurveyElementType = SurveyElementType.ANSWER

    @JsonIgnore
    override val children = answers

    init {
        if (!this.code.matches(Regex(elementType.codeRegex))) {
            throw IllegalStateException("Wrong code: $code for $elementType:${elementType.codeRegex}")
        }
    }

    override fun duplicate(
        instructionList: List<Instruction>,
        children: List<SurveyComponent>,
        errors: List<ComponentError>
    ) =
        copy(
            instructionList = instructionList, errors = errors,
            answers = children.filterIsInstance<Answer>()
        )

    override fun withParentCode(parentCode: String) = copy(code = parentCode + code)


    override fun clearErrors(): SurveyComponent {
        return copy(errors = emptyList(),
            instructionList = instructionList.map { it.clearErrors() },
            answers = answers.map { it.clearErrors() as Answer }
        )
    }
}

fun List<SurveyComponent>.withoutErrors() = filter { it.noErrors() }