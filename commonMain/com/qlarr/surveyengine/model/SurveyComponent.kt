@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package com.qlarr.surveyengine.model

import com.qlarr.surveyengine.ext.isAnswerCode
import com.qlarr.surveyengine.ext.isGroupCode
import com.qlarr.surveyengine.ext.isQuestionCode
import com.qlarr.surveyengine.ext.isSurveyCode
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*


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

@Serializable(with = SurveyComponentSerializer::class)
sealed class SurveyComponent(
    open val code: String,
    open val instructionList: List<Instruction>,
    open val errors: List<ComponentError>
) {
    abstract fun clearErrors(): SurveyComponent
    fun hasUniqueCode(): Boolean = elementType.hasUniqueCode()
    fun uniqueCode(parentCode: String) = if (elementType.hasUniqueCode()) code else parentCode + code

    @Transient
    abstract val children: List<SurveyComponent>

    @Transient
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
        val newInstruction = (newList[index] as Instruction.IsRunnable)
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

@Serializable(with = SurveyComponentSerializer::class)
data class Survey(

    override val instructionList: List<Instruction> = listOf(),
    val groups: List<Group> = listOf(),
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent("Survey", instructionList, errors) {

    @Transient
    override val elementType: SurveyElementType = SurveyElementType.SURVEY

    @Transient
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

@Serializable
enum class GroupType {
    END, GROUP
}

@Serializable(with = SurveyComponentSerializer::class)
data class Group(
    override val code: String,
    override val instructionList: List<Instruction> = listOf(),
    val questions: List<Question> = listOf(),
    val groupType: GroupType = GroupType.GROUP,
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent(code, instructionList, errors) {

    @Transient
    override val elementType: SurveyElementType = SurveyElementType.GROUP

    @Transient
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

    fun isNotEndGroup() = groupType != GroupType.END
}

@Serializable(with = SurveyComponentSerializer::class)
data class Question(
    override val code: String,
    override val instructionList: List<Instruction> = listOf(),
    val answers: List<Answer> = listOf(),
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent(code, instructionList, errors) {

    @Transient
    override val elementType: SurveyElementType = SurveyElementType.QUESTION

    @Transient
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

@Serializable(with = SurveyComponentSerializer::class)
data class Answer(
    override val code: String,
    override val instructionList: List<Instruction> = listOf(),
    val answers: List<Answer> = listOf(),
    override val errors: List<ComponentError> = listOf()
) : SurveyComponent(code, instructionList, errors) {

    @Transient
    override val elementType: SurveyElementType = SurveyElementType.ANSWER

    @Transient
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


@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = SurveyComponent::class)
object SurveyComponentSerializer : KSerializer<SurveyComponent> {
    private val json = Json { ignoreUnknownKeys = true }

    override fun serialize(encoder: Encoder, value: SurveyComponent) {
        // Convert to JsonElement first for more flexibility
        val jsonObject = buildJsonObject {
            put("code", value.code)
            put(
                "instructionList",
                json.encodeToJsonElement(ListSerializer(serializer<Instruction>()), value.instructionList)
            )


            if (value.errors.isNotEmpty()) {
                put("errors", json.encodeToJsonElement(ListSerializer(serializer<ComponentError>()), value.errors))
            }
            when (value) {
                is Answer -> {
                    put("answers", json.encodeToJsonElement(ListSerializer(serializer<Answer>()), value.answers))

                }

                is Question -> {

                    put("answers", json.encodeToJsonElement(ListSerializer(serializer<Answer>()), value.answers))
                }

                is Group -> {
                    put("questions", json.encodeToJsonElement(ListSerializer(serializer<Question>()), value.questions))
                }

                is Survey -> {
                    put("groups", json.encodeToJsonElement(ListSerializer(serializer<Group>()), value.groups))
                }

            }
        }
        (encoder as? JsonEncoder)?.encodeJsonElement(jsonObject) ?: encoder.encodeString(jsonObject.toString())
    }

    override fun deserialize(decoder: Decoder): SurveyComponent {
        // Use JsonDecoder for flexibility
        val jsonElement = when (decoder) {
            is JsonDecoder -> decoder.decodeJsonElement()
            else -> {
                // Fallback for non-JSON decoders (not ideal, but better than nothing)
                val jsonDecoder = Json.parseToJsonElement(decoder.decodeString())
                if (jsonDecoder is JsonObject) jsonDecoder else throw SerializationException("Expected JsonObject")
            }
        }

        if (jsonElement !is JsonObject) throw SerializationException("Expected JsonObject")

        val code: String =
            jsonElement["code"]?.jsonPrimitive?.contentOrNull ?: throw SerializationException("Code Missing")
        val instructionList: List<Instruction> = jsonElement["instructionList"]?.let {
            json.decodeFromJsonElement(ListSerializer(serializer<Instruction>()), it)
        } ?: listOf()
        val errors: List<ComponentError> = jsonElement["errors"]?.let {
            json.decodeFromJsonElement(ListSerializer(serializer<ComponentError>()), it)
        } ?: listOf()

        return if (code.isSurveyCode()) {
            val groups: List<Group> = jsonElement["groups"]?.let {
                json.decodeFromJsonElement(ListSerializer(serializer<Group>()), it)
            } ?: listOf()
            Survey(instructionList, groups, errors)
        } else if (code.isGroupCode()) {
            val groupType: GroupType = jsonElement["groupType"]?.jsonPrimitive?.let {
                json.decodeFromJsonElement(serializer<GroupType>(), it)
            } ?: GroupType.GROUP

            val questions: List<Question> = jsonElement["questions"]?.let {
                json.decodeFromJsonElement(ListSerializer(serializer<Question>()), it)
            } ?: listOf()
            Group(code, instructionList, questions,groupType, errors)
        } else if (code.isQuestionCode()) {
            val answers: List<Answer> = jsonElement["answers"]?.let {
                json.decodeFromJsonElement(ListSerializer(serializer<Answer>()), it)
            } ?: listOf()
            Question(code, instructionList, answers, errors)
        } else if (code.isAnswerCode()) {

            val answers: List<Answer> = jsonElement["answers"]?.let {
                json.decodeFromJsonElement(ListSerializer(serializer<Answer>()), it)
            } ?: listOf()
            Answer(code, instructionList, answers, errors)
        } else {
            throw SerializationException("Invalid component code")
        }


    }
}