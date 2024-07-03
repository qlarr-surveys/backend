package com.frankie.expressionmanager.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.frankie.expressionmanager.model.adapters.ReturnTypeGson
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "name",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(ReturnType.FrankieBoolean::class, name = "Boolean"),
    JsonSubTypes.Type(ReturnType.FrankieString::class, name = "String"),
    JsonSubTypes.Type(ReturnType.FrankieInt::class, name = "Int"),
    JsonSubTypes.Type(ReturnType.FrankieList::class, name = "List"),
    JsonSubTypes.Type(ReturnType.FrankieFile::class, name = "File"),
    JsonSubTypes.Type(ReturnType.FrankieMap::class, name = "Map"),
    JsonSubTypes.Type(ReturnType.FrankieDate::class, name = "Date"),
    JsonSubTypes.Type(ReturnType.FrankieDouble::class, name = "Double"),
)
@JsonAdapter(ReturnTypeGson::class)
sealed class ReturnType(val name: String) {
    @JsonAdapter(ReturnTypeGson::class)
    object FrankieBoolean : ReturnType("Boolean")

    @JsonAdapter(ReturnTypeGson::class)
    object FrankieString : ReturnType("String")
    @JsonAdapter(ReturnTypeGson::class)
    object FrankieInt : ReturnType("Int")
    @JsonAdapter(ReturnTypeGson::class)
    object FrankieDouble : ReturnType("Double")
    @JsonAdapter(ReturnTypeGson::class)
    object FrankieList : ReturnType("List")
    @JsonAdapter(ReturnTypeGson::class)
    object FrankieMap : ReturnType("Map")
    @JsonAdapter(ReturnTypeGson::class)
    object FrankieDate : ReturnType("Date")
    @JsonAdapter(ReturnTypeGson::class)
    object FrankieFile : ReturnType("File")

    override fun equals(other: Any?): Boolean {
        return when (this) {
            is FrankieBoolean -> other is FrankieBoolean
            is FrankieString -> other is FrankieString
            is FrankieDate -> other is FrankieDate
            is FrankieInt -> other is FrankieInt
            is FrankieDouble -> other is FrankieDouble
            is FrankieList -> other is FrankieList
            is FrankieFile -> other is FrankieFile
            is FrankieMap -> other is FrankieMap
        }
    }

    fun defaultTextValue(): String {
        return when (this) {
            FrankieList -> "[]"
            FrankieString -> ""
            FrankieBoolean -> "false"
            FrankieDate -> "1970-01-01 00:00:00"
            FrankieInt, FrankieDouble -> "0"
            FrankieMap -> "{}"
            is FrankieFile -> "{\"filename\":\"\",\"stored_filename\":\"\",\"size\":0,\"type\":\"\"}"
        }
    }

    fun toDbType(): DataType = when (this) {
        FrankieList -> DataType.LIST
        FrankieBoolean -> DataType.BOOLEAN
        FrankieDate -> DataType.DATE
        FrankieDouble -> DataType.DOUBLE
        is FrankieFile -> DataType.FILE
        FrankieInt -> DataType.INT
        FrankieMap -> DataType.MAP
        FrankieString -> DataType.STRING
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

data class TypedValue(val returnType: ReturnType, val value: Any)

