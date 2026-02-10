package com.qlarr.surveyengine.model.exposed

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@Serializable(with = NavigationDirectionSerializer::class)
@JsExport
sealed class NavigationDirection {
    abstract val name: String

    data object Start : NavigationDirection() {
        override val name: String = "START"
    }


    data object Previous : NavigationDirection() {
        override val name: String = "PREV"
    }


    data class Jump(
        val navigationIndex: NavigationIndex,
        override val name: String = "JUMP"
    ) : NavigationDirection()


    data object Next : NavigationDirection() {
        override val name: String = "NEXT"
    }


    data object Resume : NavigationDirection() {
        override val name: String = "RESUME"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = NavigationDirection::class)
object NavigationDirectionSerializer : KSerializer<NavigationDirection> {
    private val json = Json { ignoreUnknownKeys = true }

    override fun serialize(encoder: Encoder, value: NavigationDirection) {
        // Convert to JsonElement for flexibility
        val jsonObject = buildJsonObject {
            put("name", JsonPrimitive(value.name))

            when (value) {
                is NavigationDirection.Jump -> {
                    put("navigationIndex", json.encodeToJsonElement(value.navigationIndex))
                }
                // Other cases don't need additional fields
                else -> { /* No additional fields needed */
                }
            }
        }
        (encoder as JsonEncoder).encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): NavigationDirection {
        // Use JsonDecoder for flexibility
        val jsonElement = when (decoder) {
            is JsonDecoder -> decoder.decodeJsonElement()
            else -> {
                // Fallback for non-JSON decoders
                val jsonDecoder = Json.parseToJsonElement(decoder.decodeString())
                if (jsonDecoder is JsonObject) jsonDecoder else throw SerializationException("Expected JsonObject")
            }
        }

        if (jsonElement !is JsonObject) throw SerializationException("Expected JsonObject")

        val name = jsonElement["name"]?.jsonPrimitive?.contentOrNull
            ?: throw SerializationException("Navigation direction missing 'name' field")

        return when (name) {
            "START" -> NavigationDirection.Start
            "PREV" -> NavigationDirection.Previous
            "NEXT" -> NavigationDirection.Next
            "RESUME" -> NavigationDirection.Resume
            "JUMP" -> {
                val navigationIndex = jsonElement["navigationIndex"]?.let {
                    json.decodeFromJsonElement<NavigationIndex>(it)
                } ?: throw SerializationException("Jump direction missing 'navigationIndex' field")

                NavigationDirection.Jump(navigationIndex)
            }

            else -> throw SerializationException("Unknown navigation direction: $name")
        }
    }
}