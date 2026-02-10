@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package com.qlarr.surveyengine.model

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = InstructionErrorSerializer::class)
sealed class InstructionError(val name: String = "") {

    @Serializable(with = InstructionErrorSerializer::class)
    data class ForwardDependency(val dependency: Dependency) : InstructionError("ForwardDependency")
    @Serializable(with = InstructionErrorSerializer::class)
    data class ScriptError(val message: String, val start: Int, val end: Int) : InstructionError("ScriptError")
    @Serializable(with = InstructionErrorSerializer::class)
    data class InvalidSkipReference(val component: String) : InstructionError("InvalidSkipReference")
    @Serializable(with = InstructionErrorSerializer::class)
    data object DisqualifyNotToEnd : InstructionError("DisqualifyNotToEnd")
    @Serializable(with = InstructionErrorSerializer::class)
    data object SkipToEndOfEndGroup : InstructionError("SkipToEndOfEndGroup")
    @Serializable(with = InstructionErrorSerializer::class)
    data class InvalidReference(val reference: String, val invalidComponent: Boolean) :
        InstructionError("InvalidReference")

    @Serializable(with = InstructionErrorSerializer::class)
    data class InvalidChildReferences(val children: List<String>) : InstructionError("InvalidChildReferences")
    @Serializable(with = InstructionErrorSerializer::class)
    data object PriorityLimitMismatch : InstructionError("DuplicateLimitMismatch")
    @Serializable(with = InstructionErrorSerializer::class)
    data class DuplicatePriorityGroupItems(val items: List<String>) : InstructionError("DuplicatePriorityGroupItems")
    @Serializable(with = InstructionErrorSerializer::class)
    data class PriorityGroupItemNotChild(val items: List<String>) : InstructionError("PriorityGroupItemNotChild")
    @Serializable(with = InstructionErrorSerializer::class)
    data class InvalidPriorityItem(val items: List<String>) : InstructionError("InvalidPriorityItem")
    @Serializable(with = InstructionErrorSerializer::class)
    data class InvalidRandomItem(val items: List<String>) : InstructionError("InvalidRandomItem")
    @Serializable(with = InstructionErrorSerializer::class)
    data class DuplicateRandomGroupItems(val items: List<String>) : InstructionError("DuplicateRandomGroupItems")
    @Serializable(with = InstructionErrorSerializer::class)
    data class RandomGroupItemNotChild(val items: List<String>) : InstructionError("RandomGroupItemNotChild")
    @Serializable(with = InstructionErrorSerializer::class)
    data object DuplicateInstructionCode : InstructionError("DuplicateInstructionCode")
    @Serializable(with = InstructionErrorSerializer::class)
    data object InvalidInstructionInEndGroup : InstructionError("InvalidInstructionInEndGroup")

}


@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = InstructionError::class)
object InstructionErrorSerializer : KSerializer<InstructionError> {


    override fun serialize(encoder: Encoder, value: InstructionError) {
        val jsonObject = buildJsonObject {
            put("name", value.name)
            when (value) {
                is InstructionError.DuplicatePriorityGroupItems -> {
                    put("items", JsonArray(value.items.map { JsonPrimitive(it) }))
                }

                is InstructionError.DuplicateRandomGroupItems -> {
                    put("items", JsonArray(value.items.map { JsonPrimitive(it) }))
                }

                is InstructionError.ForwardDependency -> {
                    put(
                        "dependency",
                        jsonMapper.encodeToJsonElement(serializer<Dependency>(), value.dependency)
                    )

                }

                is InstructionError.InvalidChildReferences -> {
                    put("items", JsonArray(value.children.map { JsonPrimitive(it) }))
                }

                is InstructionError.InvalidPriorityItem -> {
                    put("items", JsonArray(value.items.map { JsonPrimitive(it) }))
                }

                is InstructionError.InvalidRandomItem -> {
                    put("items", JsonArray(value.items.map { JsonPrimitive(it) }))
                }

                is InstructionError.InvalidReference -> {
                    put("reference", value.reference)
                    put("invalidComponent", value.invalidComponent)
                }

                is InstructionError.InvalidSkipReference -> {
                    put("component", value.component)
                }

                is InstructionError.PriorityGroupItemNotChild -> {
                    put("items", JsonArray(value.items.map { JsonPrimitive(it) }))
                }

                is InstructionError.RandomGroupItemNotChild -> {
                    put("items", JsonArray(value.items.map { JsonPrimitive(it) }))
                }

                is InstructionError.ScriptError -> {
                    put("message", value.message)
                    put("start", value.start)
                    put("end", value.end)

                }
                InstructionError.DisqualifyNotToEnd,
                InstructionError.DuplicateInstructionCode,
                InstructionError.InvalidInstructionInEndGroup,
                InstructionError.PriorityLimitMismatch,
                InstructionError.SkipToEndOfEndGroup -> {
                    // do nothing
                }
            }
        }
        (encoder as? JsonEncoder)?.encodeJsonElement(jsonObject) ?: encoder.encodeString(jsonObject.toString())
    }

    override fun deserialize(decoder: Decoder): InstructionError {
        // Convert the input to JsonElement
        val jsonElement = when (decoder) {
            is JsonDecoder -> decoder.decodeJsonElement()
            else -> Json.parseToJsonElement(decoder.decodeString())
        }

        // Ensure we have a JsonObject
        if (jsonElement !is JsonObject) throw SerializationException("Expected JsonObject")

        // Get the error type
        val name = jsonElement["name"]?.jsonPrimitive?.content
            ?: throw SerializationException("InstructionError requires a 'name' field")

        // Create the appropriate error type based on the name
        return when (name) {
            "DuplicatePriorityGroupItems" -> {
                val items = jsonElement["items"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
                InstructionError.DuplicatePriorityGroupItems(items)
            }

            "DuplicateRandomGroupItems" -> {
                val items = jsonElement["items"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
                InstructionError.DuplicateRandomGroupItems(items)
            }

            "ForwardDependency" -> {
                val dependencyJson = jsonElement["dependency"]
                    ?: throw SerializationException("ForwardDependency requires a 'dependency' field")
                val dependency = jsonMapper.decodeFromJsonElement(serializer<Dependency>(), dependencyJson)
                InstructionError.ForwardDependency(dependency)
            }

            "InvalidChildReferences" -> {
                val children = jsonElement["items"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
                InstructionError.InvalidChildReferences(children)
            }

            "InvalidPriorityItem" -> {
                val items = jsonElement["items"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
                InstructionError.InvalidPriorityItem(items)
            }

            "InvalidRandomItem" -> {
                val items = jsonElement["items"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
                InstructionError.InvalidRandomItem(items)
            }

            "InvalidReference" -> {
                val reference = jsonElement["reference"]?.jsonPrimitive?.content
                    ?: throw SerializationException("InvalidReference requires a 'reference' field")
                val invalidComponent = jsonElement["invalidComponent"]?.jsonPrimitive?.booleanOrNull
                    ?: throw SerializationException("InvalidReference requires an 'invalidComponent' field")
                InstructionError.InvalidReference(reference, invalidComponent)
            }

            "InvalidSkipReference" -> {
                val component = jsonElement["component"]?.jsonPrimitive?.content
                    ?: throw SerializationException("InvalidSkipReference requires a 'component' field")
                InstructionError.InvalidSkipReference(component)
            }

            "PriorityGroupItemNotChild" -> {
                val items = jsonElement["items"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
                InstructionError.PriorityGroupItemNotChild(items)
            }

            "RandomGroupItemNotChild" -> {
                val items = jsonElement["items"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
                InstructionError.RandomGroupItemNotChild(items)
            }

            "ScriptError" -> {
                val message = jsonElement["message"]?.jsonPrimitive?.content
                    ?: throw SerializationException("ScriptError requires a 'message' field")
                val start = jsonElement["start"]?.jsonPrimitive?.intOrNull
                    ?: throw SerializationException("ScriptError requires a 'start' field")
                val end = jsonElement["end"]?.jsonPrimitive?.intOrNull
                    ?: throw SerializationException("ScriptError requires an 'end' field")
                InstructionError.ScriptError(message, start, end)
            }

            "DisqualifyNotToEnd" -> InstructionError.DisqualifyNotToEnd
            "DuplicateInstructionCode" -> InstructionError.DuplicateInstructionCode

            "InvalidInstructionInEndGroup" -> InstructionError.InvalidInstructionInEndGroup

            "PriorityLimitMismatch" -> InstructionError.PriorityLimitMismatch

            "SkipToEndOfEndGroup" -> InstructionError.SkipToEndOfEndGroup

            else -> throw SerializationException("Unknown InstructionError type: $name")
        }
    }
}