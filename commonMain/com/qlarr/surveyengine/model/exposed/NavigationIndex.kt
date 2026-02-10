@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package com.qlarr.surveyengine.model.exposed

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

fun NavigationIndex.stringIndex() = when (this) {
    is NavigationIndex.End -> "End"
    is NavigationIndex.Group -> this.groupId
    is NavigationIndex.Groups -> this.groupIds.toString()
    is NavigationIndex.Question -> this.questionId
}

@OptIn(ExperimentalJsExport::class)
@Serializable(with = NavigationIndexSerializer::class)
@JsExport
sealed class NavigationIndex {

    fun navigationMode(): NavigationMode? = when (this) {
        is Question -> NavigationMode.QUESTION_BY_QUESTION
        is End -> null
        is Group -> NavigationMode.GROUP_BY_GROUP
        is Groups -> NavigationMode.ALL_IN_ONE
    }

    abstract val name: String
    @Transient
    abstract val showError: Boolean
    abstract fun with(showError: Boolean): NavigationIndex


    @Serializable(with = NavigationIndexSerializer::class)
    data class Groups(
        val groupIds: List<String>, 
        @Transient override val showError: Boolean = false,
        override val name: String = "groups"
    ) : NavigationIndex() {
        override fun with(showError: Boolean) = copy(showError = showError)
    }


    @Serializable(with = NavigationIndexSerializer::class)
    data class Group(
        val groupId: String, 
        @Transient override val showError: Boolean = false,
        override val name: String = "group"
    ) : NavigationIndex() {
        override fun with(showError: Boolean) = copy(showError = showError)
    }


    @Serializable(with = NavigationIndexSerializer::class)
    data class Question(
        val questionId: String, 
        @Transient override val showError: Boolean = false,
        override val name: String = "question"
    ) : NavigationIndex() {
        override fun with(showError: Boolean) = copy(showError = showError)
    }


    @Serializable(with = NavigationIndexSerializer::class)
    data class End(
        val groupId: String,
        override val name: String = "end"
    ) : NavigationIndex() {
        @Transient
        override val showError: Boolean = false
        
        override fun with(showError: Boolean): NavigationIndex {
            if (showError) throw IllegalArgumentException("Showing Error at GroupEnd!!!")
            return this
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = NavigationIndex::class)
object NavigationIndexSerializer : KSerializer<NavigationIndex> {
    override fun serialize(encoder: Encoder, value: NavigationIndex) {
        val jsonObject = buildJsonObject {
            put("name", JsonPrimitive(value.name))

            when (value) {
                is NavigationIndex.Groups -> {
                    put("groupIds", JsonArray(value.groupIds.map { JsonPrimitive(it) }))
                }
                is NavigationIndex.Group -> {
                    put("groupId", JsonPrimitive(value.groupId))
                }
                is NavigationIndex.Question -> {
                    put("questionId", JsonPrimitive(value.questionId))
                }
                is NavigationIndex.End -> {
                    put("groupId", JsonPrimitive(value.groupId))
                }
            }
            // Note: showError is marked @Transient so we don't serialize it
        }
        (encoder as JsonEncoder).encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): NavigationIndex {
        val jsonElement = when (decoder) {
            is JsonDecoder -> decoder.decodeJsonElement()
            else -> {
                val jsonDecoder = Json.parseToJsonElement(decoder.decodeString())
                if (jsonDecoder is JsonObject) jsonDecoder else throw SerializationException("Expected JsonObject")
            }
        }

        if (jsonElement !is JsonObject) throw SerializationException("Expected JsonObject")

        val name = jsonElement["name"]?.jsonPrimitive?.contentOrNull
            ?: throw SerializationException("Navigation index missing 'name' field")

        return when (name) {
            "groups" -> {
                val groupIds = jsonElement["groupIds"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: throw SerializationException("Groups index missing 'groupIds' field")
                NavigationIndex.Groups(groupIds)
            }
            "group" -> {
                val groupId = jsonElement["groupId"]?.jsonPrimitive?.contentOrNull
                    ?: throw SerializationException("Group index missing 'groupId' field")
                NavigationIndex.Group(groupId)
            }
            "question" -> {
                val questionId = jsonElement["questionId"]?.jsonPrimitive?.contentOrNull
                    ?: throw SerializationException("Question index missing 'questionId' field")
                NavigationIndex.Question(questionId)
            }
            "end" -> {
                val groupId = jsonElement["groupId"]?.jsonPrimitive?.contentOrNull
                    ?: throw SerializationException("End index missing 'groupId' field")
                NavigationIndex.End(groupId)
            }
            else -> throw SerializationException("Unknown navigation index: $name")
        }
    }
}





