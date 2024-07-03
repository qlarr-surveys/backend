package com.frankie.expressionmanager.model.adapters

import com.frankie.expressionmanager.model.ReturnType
import com.google.gson.*
import java.lang.reflect.Type


class ReturnTypeGson : JsonDeserializer<ReturnType>, JsonSerializer<ReturnType> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ReturnType {
        var name = ""
        val jsonObject: JsonObject = json.asJsonObject
        jsonObject.keySet().forEach { key ->
            when (key) {
                "name" -> name = jsonObject[key].asString
            }
        }
        return when (name) {
            "Boolean" -> ReturnType.FrankieBoolean
            "String" -> ReturnType.FrankieString
            "Int" -> ReturnType.FrankieInt
            "List" -> ReturnType.FrankieList
            "File" -> ReturnType.FrankieFile
            "Map" -> ReturnType.FrankieMap
            "Date" -> ReturnType.FrankieDate
            "Double" -> ReturnType.FrankieDouble
            else -> throw IllegalStateException("unidentified Return Type with name: $name")
        }
    }


    override fun serialize(src: ReturnType, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", src.name)
        return jsonObject
    }

}