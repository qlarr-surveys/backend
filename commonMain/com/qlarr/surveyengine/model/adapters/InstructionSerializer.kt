package com.qlarr.surveyengine.model.adapters

import com.qlarr.surveyengine.ext.VALID_FORMAT_INSTRUCTION_PATTERN
import com.qlarr.surveyengine.model.*
import com.qlarr.surveyengine.model.Instruction.*
import com.qlarr.surveyengine.model.exposed.ReturnType
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Instruction::class)
object InstructionSerializer : KSerializer<Instruction> {
    private val json = Json { ignoreUnknownKeys = true }

    override fun serialize(encoder: Encoder, value: Instruction) {
        // Convert to JsonElement first for more flexibility
        val jsonObject = buildJsonObject {
            put("code", value.code)

            when (value) {
                is Format -> {
                    put("contentPath", JsonArray(value.contentPath.map { JsonPrimitive(it) }))
                    put("lang", value.lang)
                    put("text", value.text)
                }

                is RandomGroups -> {
                    put("groups", json.encodeToJsonElement(ListSerializer(serializer<RandomGroup>()), value.groups))
                }

                is PriorityGroups -> {
                    put(
                        "priorities",
                        json.encodeToJsonElement(ListSerializer(serializer<PriorityGroup>()), value.priorities)
                    )
                }

                is ParentRelevance -> {
                    put("children", JsonArray(value.children.map { childList ->
                        JsonArray(childList.map { JsonPrimitive(it) })
                    }))
                }

                is State -> {
                    put("text", value.text)
                    put("returnType", json.encodeToJsonElement(serializer<ReturnType>(), value.returnType))
                    put("isActive", value.isActive)

                    if (value is SkipInstruction) {
                        put("skipToComponent", value.skipToComponent)
                        put("toEnd", value.toEnd)
                        put("disqualify", value.disqualify)
                    }
                }
            }

            if (value.errors.isNotEmpty()) {
                put("errors", json.encodeToJsonElement(ListSerializer(serializer<InstructionError>()), value.errors))
            }
        }
        (encoder as JsonEncoder).encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Instruction {
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

        val code = jsonElement["code"]?.jsonPrimitive?.contentOrNull ?: ""
        val errors = jsonElement["errors"]?.let {
            json.decodeFromJsonElement(ListSerializer(serializer<InstructionError>()), it)
        } ?: listOf()

        // Extract fields directly from JSON
        val text = jsonElement["text"]?.jsonPrimitive?.contentOrNull
        val lang = jsonElement["lang"]?.jsonPrimitive?.contentOrNull
        val isActive = jsonElement["isActive"]?.jsonPrimitive?.booleanOrNull
        val skipToComponent = jsonElement["skipToComponent"]?.jsonPrimitive?.contentOrNull ?: ""
        val toEnd = jsonElement["toEnd"]?.jsonPrimitive?.booleanOrNull ?: false
        val disqualify = jsonElement["disqualify"]?.jsonPrimitive?.booleanOrNull ?: false

        val returnType: ReturnType? = jsonElement["returnType"]?.let { returnTypeElement ->
            json.decodeFromJsonElement(serializer(), returnTypeElement)
        }

        // Extract specialized fields
        val contentPath = jsonElement["contentPath"]?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf()

        val groups = jsonElement["groups"]?.let {
            json.decodeFromJsonElement(ListSerializer(serializer<RandomGroup>()), it)
        } ?: listOf()

        val priorities = jsonElement["priorities"]?.let {
            json.decodeFromJsonElement(ListSerializer(serializer<PriorityGroup>()), it)
        } ?: listOf()

        val children = jsonElement["children"]?.jsonArray?.map { childArray ->
            childArray.jsonArray.map { it.jsonPrimitive.content }
        } ?: listOf()

        return when {
            code.matches(Regex(SKIP_INSTRUCTION_PATTERN)) -> {
                val reservedCode = code.toReservedCode()
                SkipInstruction(
                    code = code,
                    skipToComponent = skipToComponent,
                    toEnd = toEnd,
                    disqualify = disqualify,
                    text = text ?: (returnType?.defaultTextValue() ?: reservedCode.defaultReturnType()
                        .defaultTextValue()),
                    isActive = isActive ?: reservedCode.defaultIsActive(),
                    errors = errors
                )
            }

            code.isReservedCode() -> {
                val reservedCode = code.toReservedCode()
                SimpleState(
                    text = text ?: (returnType?.defaultTextValue() ?: reservedCode.defaultReturnType()
                        .defaultTextValue()),
                    reservedCode,
                    returnType = returnType ?: reservedCode.defaultReturnType(),
                    isActive = isActive ?: reservedCode.defaultIsActive(),
                    errors = errors
                )
            }

            code.matches(Regex(VALID_FORMAT_INSTRUCTION_PATTERN)) -> {
                Format(
                    code = code,
                    contentPath = contentPath,
                    text = text ?: "" ,
                    lang = lang,
                    errors = errors
                )
            }

            code == Instruction.RANDOM_GROUP -> {
                RandomGroups(groups = groups, errors = errors)
            }

            code == Instruction.PRIORITY_GROUPS -> {
                PriorityGroups(priorities = priorities, errors = errors)
            }

            code == Instruction.PARENT_RELEVANCE -> {
                ParentRelevance(children = children, errors = errors)
            }

            else -> throw SerializationException("Invalid JSON for instruction")
        }
    }
}