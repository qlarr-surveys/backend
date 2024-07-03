package com.frankie.expressionmanager.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

@JsonDeserialize(using = ReservedCodeDeserializer::class)
@JsonSerialize(using = ReservedCodeSerializer::class)
@JsonAdapter(ReservedCodeGson::class)
sealed class ReservedCode(
    open val code: String,
    val executionOrder: Int = 100,
    val isAccessible: Boolean = false,
    val accessibleByChildren: Boolean = false,
    // will ever have a state function that is runnable
    val isRuntime: Boolean = true
) {
    @JsonAdapter(ReservedCodeGson::class)
    object Lang :
        ReservedCode("lang", executionOrder = 1, isAccessible = true, accessibleByChildren = true, isRuntime = false)

    @JsonAdapter(ReservedCodeGson::class)
    object Mode :
        ReservedCode("mode", executionOrder = 1, isAccessible = true, accessibleByChildren = true, isRuntime = false)

    @JsonAdapter(ReservedCodeGson::class)
    object Prioritised : ReservedCode("prioritised", executionOrder = 1)

    @JsonAdapter(ReservedCodeGson::class)
    object NotSkipped : ReservedCode("not_skipped", executionOrder = 1)

    @JsonAdapter(ReservedCodeGson::class)
    object ConditionalRelevance : ReservedCode("conditional_relevance", executionOrder = 1)

    @JsonAdapter(ReservedCodeGson::class)
    object ChildrenRelevance : ReservedCode("children_relevance", executionOrder = 1)

    @JsonAdapter(ReservedCodeGson::class)
    object Relevance : ReservedCode("relevance", executionOrder = 2, true)

    @JsonAdapter(ReservedCodeGson::class)
    object Value : ReservedCode("value", executionOrder = 3, true, true)

    @JsonAdapter(ReservedCodeGson::class)
    data class ValidationRule(override val code: String) : ReservedCode(code, executionOrder = 5)

    @JsonAdapter(ReservedCodeGson::class)
    object Validity : ReservedCode("validity", executionOrder = 6, true)

    @JsonAdapter(ReservedCodeGson::class)
    data class Skip(override val code: String) : ReservedCode(code, executionOrder = 7, true)

    @JsonAdapter(ReservedCodeGson::class)
    object MaskedValue : ReservedCode("masked_value", isAccessible = true)

    @JsonAdapter(ReservedCodeGson::class)
    object RelevanceMap : ReservedCode("relevance_map", executionOrder = 8)

    @JsonAdapter(ReservedCodeGson::class)
    object ValidityMap : ReservedCode("validity_map", executionOrder = 8)

    @JsonAdapter(ReservedCodeGson::class)
    object BeforeNavigation : ReservedCode("before_navigation", executionOrder = 8)

    @JsonAdapter(ReservedCodeGson::class)
    object AfterNavigation : ReservedCode("after_navigation", executionOrder = 8)

    @JsonAdapter(ReservedCodeGson::class)
    object Order : ReservedCode("order", isAccessible = true, isRuntime = false)

    @JsonAdapter(ReservedCodeGson::class)
    object Priority : ReservedCode("priority", isAccessible = true, isRuntime = false)

    @JsonAdapter(ReservedCodeGson::class)
    object ShowErrors : ReservedCode("show_errors", isRuntime = false)

    @JsonAdapter(ReservedCodeGson::class)
    object HasPrevious : ReservedCode("has_previous")

    @JsonAdapter(ReservedCodeGson::class)
    object HasNext : ReservedCode("has_next")

    @JsonAdapter(ReservedCodeGson::class)
    object Meta : ReservedCode("meta", isRuntime = false)

    @JsonAdapter(ReservedCodeGson::class)
    object Label : ReservedCode("label", isRuntime = false, isAccessible = true, accessibleByChildren = true)

    @JsonAdapter(ReservedCodeGson::class)
    object InCurrentNavigation : ReservedCode("in_current_navigation", isRuntime = false, isAccessible = true)

    fun defaultReturnType(): ReturnType {
        return when (this) {
            is Order, is Priority -> ReturnType.FrankieInt
            is Meta, RelevanceMap, ValidityMap -> ReturnType.FrankieMap
            is BeforeNavigation, AfterNavigation -> ReturnType.FrankieList
            is Lang, is Mode, is Value, is MaskedValue, is Label -> ReturnType.FrankieString
            is Relevance, is Prioritised, is NotSkipped, is ConditionalRelevance, is ChildrenRelevance, is InCurrentNavigation,
            is Skip, is Validity, is ValidationRule, is ShowErrors, is HasPrevious, is HasNext -> ReturnType.FrankieBoolean
        }
    }


    fun defaultIsActive(): Boolean {
        return when (this) {
            Order, Value, Meta, ShowErrors, Lang, Mode, Priority, Label, InCurrentNavigation -> false
            else -> true
        }
    }
}

class ReservedCodeSerializer : StdSerializer<ReservedCode>(ReservedCode::class.java) {
    override fun serialize(value: ReservedCode, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.code)
    }

}

class ReservedCodeDeserializer : StdDeserializer<ReservedCode>(ReservedCode::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ReservedCode {
        return p.text.toReservedCode()
    }
}


const val VALIDATION_PREFIX = "validation_"
const val VALIDATION_INSTRUCTION_PATTERN = "$VALIDATION_PREFIX[a-z0-9][a-z0-9_]*\$"
const val SKIP_PREFIX = "skip_to_"
const val SKIP_INSTRUCTION_PATTERN = "$SKIP_PREFIX[A-Za-z0-9][A-Za-z0-9_]*\$"

fun String.toReservedCode(): ReservedCode {
    return when {
        this == "lang" -> ReservedCode.Lang
        this == "mode" -> ReservedCode.Mode
        this == "prioritised" -> ReservedCode.Prioritised
        this == "not_skipped" -> ReservedCode.NotSkipped
        this == "order" -> ReservedCode.Order
        this == "priority" -> ReservedCode.Priority
        this == "meta" -> ReservedCode.Meta
        this == "show_errors" -> ReservedCode.ShowErrors
        this == "value" -> ReservedCode.Value
        this == "relevance" -> ReservedCode.Relevance
        this == "conditional_relevance" -> ReservedCode.ConditionalRelevance
        this == "children_relevance" -> ReservedCode.ChildrenRelevance
        this == "validity" -> ReservedCode.Validity
        this == "has_previous" -> ReservedCode.HasPrevious
        this == "has_next" -> ReservedCode.HasNext
        this == "masked_value" -> ReservedCode.MaskedValue
        this == "label" -> ReservedCode.Label
        this == "in_current_navigation" -> ReservedCode.InCurrentNavigation
        this == "before_navigation" -> ReservedCode.BeforeNavigation
        this == "after_navigation" -> ReservedCode.AfterNavigation
        this == "relevance_map" -> ReservedCode.RelevanceMap
        this == "validity_map" -> ReservedCode.ValidityMap
        this.matches(Regex(VALIDATION_INSTRUCTION_PATTERN)) -> ReservedCode.ValidationRule(this)
        this.matches(Regex(SKIP_INSTRUCTION_PATTERN)) -> ReservedCode.Skip(this)
        else -> throw IllegalStateException("")
    }
}

fun String.isReservedCode(): Boolean {
    return this in listOf(
        "lang",
        "mode",
        "prioritised",
        "not_skipped",
        "order",
        "priority",
        "meta",
        "show_errors",
        "value",
        "relevance",
        "children_relevance",
        "conditional_relevance",
        "validity",
        "has_previous",
        "has_next",
        "masked_value",
        "relevance_map",
        "validity_map",
        "label",
        "in_current_navigation",
        "before_navigation",
        "after_navigation"
    )
            || this.matches(Regex(VALIDATION_INSTRUCTION_PATTERN))
            || this.matches(Regex(SKIP_INSTRUCTION_PATTERN))
}

class ReservedCodeGson : JsonDeserializer<ReservedCode>, JsonSerializer<ReservedCode> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ReservedCode {
        return json.asString.toReservedCode()
    }

    override fun serialize(src: ReservedCode, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.code)
    }
}

enum class SurveyMode {
    OFFLINE, ONLINE
}