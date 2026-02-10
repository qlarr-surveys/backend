package com.qlarr.surveyengine.model.exposed

import com.qlarr.surveyengine.model.adapters.ReturnTypeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport


@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable(with = ReturnTypeSerializer::class)
sealed class ReturnType {
    @Serializable(with = ReturnTypeSerializer::class)
    data object Boolean : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data object String : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data object Int : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data object Double : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data object Map : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data object Date : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data object File : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data class Enum(val values: Set<kotlin.String>) : ReturnType()

    @Serializable(with = ReturnTypeSerializer::class)
    data class List(val values: Set<kotlin.String>) : ReturnType()

    fun defaultTextValue(): kotlin.String {
        return when (this) {
            is List -> "[]"
            is Enum,
            String -> ""

            Boolean -> "false"
            Date -> "1970-01-01 00:00:00"
            Int, Double -> "0"
            Map -> "{}"
            File -> "{\"filename\":\"\",\"stored_filename\":\"\",\"size\":0,\"type\":\"\"}"
        }
    }

    fun containsChildCode(childCode: kotlin.String) =
        (this is Enum && values.contains(childCode)) || (this is List && values.contains(childCode))

    fun replaceChildCode(childCode: kotlin.String, newCode: kotlin.String) =
        if (this is Enum && values.contains(childCode)) {
            Enum(values.map { if (it == childCode) newCode else it }.toSet())
        } else if (this is List && values.contains(childCode)) {
            List(values.map { if (it == childCode) newCode else it }.toSet())
        } else {
            this
        }
}

@Serializable
data class TypedValue(val returnType: ReturnType, val value: JsonElement)

