package com.frankie.expressionmanager.model.adapters

import com.frankie.expressionmanager.model.NavigationDirection
import com.frankie.expressionmanager.model.NavigationIndex
import com.google.gson.*
import java.lang.reflect.Type


class NavigationDirectionGson : JsonDeserializer<NavigationDirection>, JsonSerializer<NavigationDirection> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): NavigationDirection {
        var name = ""
        var navigationIndex: NavigationIndex? = null
        val jsonObject: JsonObject = json.asJsonObject
        jsonObject.keySet().forEach { key ->
            when (key) {
                "name" -> name = jsonObject[key].asString
                "navigationIndex" -> navigationIndex = Gson().fromJson(jsonObject[key], NavigationIndex::class.java)
            }
        }
        return when (name) {
            "START" -> NavigationDirection.Start
            "RESUME" -> NavigationDirection.Resume
            "CHANGE_LANGE" -> NavigationDirection.ChangeLange
            "PREV" -> NavigationDirection.Previous
            "JUMP" -> NavigationDirection.Jump(navigationIndex!!)
            "NEXT" -> NavigationDirection.Next
            else -> throw IllegalStateException("invalid name for NavigationDirection: $name")
        }
    }

    override fun serialize(src: NavigationDirection, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", src.name)
        if (src is NavigationDirection.Jump) {
            jsonObject.add("navigationIndex", Gson().toJsonTree(src.navigationIndex))
        }
        return jsonObject
    }

}


