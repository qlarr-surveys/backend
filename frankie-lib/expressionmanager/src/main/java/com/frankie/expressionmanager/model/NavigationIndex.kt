package com.frankie.expressionmanager.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.frankie.expressionmanager.model.adapters.NavigationDirectionGson
import com.frankie.expressionmanager.model.adapters.NavigationIndexGson
import com.google.gson.annotations.JsonAdapter

fun NavigationIndex.stringIndex() = when (this) {
    is NavigationIndex.End -> "End"
    is NavigationIndex.Group -> this.groupId
    is NavigationIndex.Groups -> this.groupIds.toString()
    is NavigationIndex.Question -> this.questionId
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "name",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(NavigationIndex.Groups::class, name = "groups"),
    JsonSubTypes.Type(NavigationIndex.Group::class, name = "group"),
    JsonSubTypes.Type(NavigationIndex.Question::class, name = "question"),
    JsonSubTypes.Type(NavigationIndex.End::class, name = "end"),
)
@JsonAdapter(NavigationIndexGson::class)
sealed class NavigationIndex(open val name: String, @JsonIgnore @Transient open val showError: Boolean) {
    abstract fun with(showError: Boolean): NavigationIndex

    @JsonAdapter(NavigationIndexGson::class)
    data class Groups(val groupIds: List<String>, @JsonIgnore @Transient override val showError: Boolean = false) :
        NavigationIndex("groups", showError) {
        override fun with(showError: Boolean) = copy(showError = showError)
    }

    @JsonAdapter(NavigationIndexGson::class)
    data class Group(val groupId: String, @JsonIgnore @Transient override val showError: Boolean = false) :
        NavigationIndex("group", showError) {
        override fun with(showError: Boolean) = copy(showError = showError)
    }

    @JsonAdapter(NavigationIndexGson::class)
    data class Question(val questionId: String, @JsonIgnore @Transient override val showError: Boolean = false) :
        NavigationIndex("question", showError) {
        override fun with(showError: Boolean) = copy(showError = showError)
    }

    @JsonAdapter(NavigationIndexGson::class)
    data class End(val groupId: String) : NavigationIndex("end", false) {
        override fun with(showError: Boolean): NavigationIndex {
            if (showError) throw IllegalArgumentException("Showing Error at GroupEnd!!!")
            return this
        }
    }
}

@JsonDeserialize(using = NavigationDirectionDeserializer::class)
@JsonSerialize(using = NavigationDirectionSerializer::class)
@JsonAdapter(NavigationDirectionGson::class)
sealed class NavigationDirection(val name: String) {
    @JsonAdapter(NavigationDirectionGson::class)
    object Start : NavigationDirection("START")

    @JsonAdapter(NavigationDirectionGson::class)
    object Previous : NavigationDirection("PREV")

    @JsonAdapter(NavigationDirectionGson::class)
    data class Jump(val navigationIndex: NavigationIndex) : NavigationDirection("JUMP")

    @JsonAdapter(NavigationDirectionGson::class)
    object Next : NavigationDirection("NEXT")

    @JsonAdapter(NavigationDirectionGson::class)
    object Resume : NavigationDirection("RESUME")

    @JsonAdapter(NavigationDirectionGson::class)
    object ChangeLange : NavigationDirection("CHANGE_LANGE")
}

class NavigationDirectionDeserializer : StdDeserializer<NavigationDirection>(NavigationDirection::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): NavigationDirection {
        val node: JsonNode = p.codec.readTree(p)
        var navigationIndex: NavigationIndex? = null
        var objName = ""
        node.fieldNames().forEach { name ->
            when (name) {
                "name" -> objName = node[name].textValue()
                "navigationIndex" -> navigationIndex = jacksonKtMapper.treeToValue(node[name])
            }
        }
        return when (objName) {
            "START" -> NavigationDirection.Start
            "RESUME" -> NavigationDirection.Resume
            "CHANGE_LANGE" -> NavigationDirection.ChangeLange
            "PREV" -> NavigationDirection.Previous
            "JUMP" -> NavigationDirection.Jump(navigationIndex!!)
            "NEXT" -> NavigationDirection.Next
            else -> throw IllegalStateException("invalid name for NavigationDirection")
        }
    }

}

class NavigationDirectionSerializer : StdSerializer<NavigationDirection>(NavigationDirection::class.java) {
    override fun serialize(value: NavigationDirection, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("name", value.name)
        if (value is NavigationDirection.Jump) {
            gen.writeObjectField("navigationIndex", value.navigationIndex)
        }
        gen.writeEndObject()
    }
}

enum class NavigationMode {
    ALL_IN_ONE,
    GROUP_BY_GROUP,
    QUESTION_BY_QUESTION;

    companion object {
        fun fromString(string: String?) = when (string?.lowercase()) {
            ALL_IN_ONE.name.lowercase() -> ALL_IN_ONE
            QUESTION_BY_QUESTION.name.lowercase() -> QUESTION_BY_QUESTION
            else -> GROUP_BY_GROUP
        }
    }


}