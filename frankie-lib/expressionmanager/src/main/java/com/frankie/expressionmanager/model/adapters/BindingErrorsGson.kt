package com.frankie.expressionmanager.model.adapters

import com.frankie.expressionmanager.model.BindingErrors
import com.frankie.expressionmanager.model.Dependency
import com.frankie.expressionmanager.model.ScriptResult
import com.google.gson.*
import java.lang.reflect.Type


class BindingErrorsGson : JsonDeserializer<BindingErrors>, JsonSerializer<BindingErrors> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BindingErrors {
        var name = ""
        var component = ""
        var reference = ""
        var invalidComponent: Boolean? = null
        var dependency: Dependency? = null
        var scriptFailure: ScriptResult? = null
        var items: List<String> = listOf()
        var children: List<String> = listOf()
        val jsonObject: JsonObject = json.asJsonObject
        jsonObject.keySet().forEach { key ->
            when (key) {
                "name" -> name = jsonObject[key].asString
                "component" -> component = jsonObject[key].asString
                "reference" -> reference = jsonObject[key].asString
                "items" -> items = jsonObject[key].asJsonArray.map { it.asString }
                "dependency" -> dependency = Gson().fromJson(jsonObject[key], Dependency::class.java)
                "invalidComponent" -> invalidComponent = jsonObject[key].asBoolean
                "scriptFailure" -> scriptFailure = Gson().fromJson(jsonObject[key], ScriptResult::class.java)
                "children" -> children = jsonObject[key].asJsonArray.map { it.asString }
            }
        }
        return when (name) {
            "question" -> BindingErrors.SkipToEndOfEndGroup
            "DuplicateInstructionCode" -> BindingErrors.DuplicateInstructionCode
            "PriorityLimitMismatch" -> BindingErrors.PriorityLimitMismatch
            "InvalidInstructionInEndGroup" -> BindingErrors.InvalidInstructionInEndGroup
            "InvalidSkipReference" -> BindingErrors.InvalidSkipReference(component)
            "ScriptFailure" -> BindingErrors.ScriptFailure(scriptFailure!!)
            "ForwardDependency" -> BindingErrors.ForwardDependency(dependency!!)
            "InvalidReference" -> BindingErrors.InvalidReference(reference, invalidComponent!!)
            "InvalidChildReferences" -> BindingErrors.InvalidChildReferences(children)
            "InvalidRandomItem" -> BindingErrors.InvalidRandomItem(items)
            "InvalidPriorityItem" -> BindingErrors.InvalidPriorityItem(items)
            "DuplicatePriorityGroupItems" -> BindingErrors.DuplicatePriorityGroupItems(items)
            "DuplicateRandomGroupItems" -> BindingErrors.DuplicateRandomGroupItems(items)
            "PriorityGroupItemNotChild" -> BindingErrors.PriorityGroupItemNotChild(items)
            "RandomGroupItemNotChild" -> BindingErrors.RandomGroupItemNotChild(items)
            else -> throw IllegalStateException("unidentified BindingErrors with name: $name")
        }
    }

    override fun serialize(src: BindingErrors, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        val gson = Gson()
        when (src) {
            is BindingErrors.DuplicatePriorityGroupItems -> {
                jsonObject.add("items", gson.toJsonTree(src.items))
            }

            is BindingErrors.DuplicateRandomGroupItems -> {
                jsonObject.add("items", gson.toJsonTree(src.items))
            }

            is BindingErrors.ForwardDependency -> {
                jsonObject.add("dependency", gson.toJsonTree(src.dependency))
            }
            is BindingErrors.InvalidChildReferences -> {
                jsonObject.add("children", gson.toJsonTree(src.children))
            }
            is BindingErrors.InvalidPriorityItem -> {
                jsonObject.add("items", gson.toJsonTree(src.items))
            }

            is BindingErrors.InvalidRandomItem -> {
                jsonObject.add("items", gson.toJsonTree(src.items))
            }

            is BindingErrors.InvalidReference -> {
                jsonObject.addProperty("invalidComponent", src.invalidComponent)
                jsonObject.addProperty("reference", src.reference)
            }

            is BindingErrors.InvalidSkipReference -> {
                jsonObject.addProperty("component", src.component)
            }

            is BindingErrors.PriorityGroupItemNotChild -> {
                jsonObject.add("items", gson.toJsonTree(src.items))
            }

            is BindingErrors.RandomGroupItemNotChild -> {
                jsonObject.add("items", gson.toJsonTree(src.items))
            }

            is BindingErrors.ScriptFailure -> {
                jsonObject.add("scriptFailure", gson.toJsonTree(src.scriptFailure))
            }

            BindingErrors.DuplicateInstructionCode,
            BindingErrors.InvalidInstructionInEndGroup,
            BindingErrors.PriorityLimitMismatch,
            BindingErrors.SkipToEndOfEndGroup -> {
                // do nothing
            }
        }
        jsonObject.addProperty("name", src.name)
        return jsonObject
    }

}


