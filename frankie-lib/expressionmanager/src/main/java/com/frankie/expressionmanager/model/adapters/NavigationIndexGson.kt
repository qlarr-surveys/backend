package com.frankie.expressionmanager.model.adapters

import com.frankie.expressionmanager.model.NavigationIndex
import com.google.gson.*
import java.lang.reflect.Type


class NavigationIndexGson : JsonDeserializer<NavigationIndex>, JsonSerializer<NavigationIndex> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): NavigationIndex {
        var name = ""
        var groupId = ""
        var questionId = ""
        var groupIds: List<String> = listOf()
        val jsonObject: JsonObject = json.asJsonObject
        jsonObject.keySet().forEach { key ->
            when (key) {
                "name" -> name = jsonObject[key].asString
                "groupId" -> groupId = jsonObject[key].asString
                "questionId" -> questionId = jsonObject[key].asString
                "groupIds" -> groupIds = jsonObject[key].asJsonArray.map { it.asString }
            }
        }
        return when (jsonObject["name"].asString) {
            "question" -> NavigationIndex.Question(questionId)
            "groups" -> NavigationIndex.Groups(groupIds)
            "group" -> NavigationIndex.Group(groupId)
            "end" -> NavigationIndex.End(groupId)
            else -> throw IllegalStateException("unidentified NavigationIndex with name: $name")
        }
    }

    override fun serialize(src: NavigationIndex, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        when (src) {
            is NavigationIndex.End -> jsonObject.addProperty("groupId", src.groupId)
            is NavigationIndex.Group -> jsonObject.addProperty("groupId", src.groupId)
            is NavigationIndex.Groups -> jsonObject.add("groupIds", JsonArray().apply { src.groupIds.forEach { add(it) } })
            is NavigationIndex.Question -> jsonObject.addProperty("questionId", src.questionId)
        }
        jsonObject.addProperty("name", src.name)
        return jsonObject
    }

}


