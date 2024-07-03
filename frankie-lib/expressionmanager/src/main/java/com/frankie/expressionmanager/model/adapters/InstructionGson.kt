package com.frankie.expressionmanager.model.adapters

import com.frankie.expressionmanager.ext.VALID_REFERENCE_INSTRUCTION_PATTERN
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.*
import com.google.gson.*
import java.lang.reflect.Type


class InstructionGson : JsonDeserializer<Instruction>, JsonSerializer<Instruction> {
    private val gson = Gson()

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instruction {

        val jsonObject: JsonObject = json.asJsonObject
        var code = ""
        var errors: List<BindingErrors> = listOf()
        var groups: List<RandomGroup> = listOf()
        var references: List<String> = listOf()
        var priorities: List<PriorityGroup> = listOf()
        var children: List<List<String>> = listOf()
        var text: String? = null
        var lang: String? = null
        var condition: String? = null
        var isActive: Boolean? = null
        var toEnd = false
        var skipToComponent = ""
        var returnType: ReturnType? = null
        jsonObject.keySet().forEach { name ->
            when (name) {
                "code" -> code = jsonObject[name].asString
                "lang" -> lang = jsonObject[name].asString
                "condition" -> condition = jsonObject[name].asString
                "errors" -> errors = jsonObject[name].asJsonArray.map { gson.fromJson(it, BindingErrors::class.java) }
                "priorities" -> priorities =
                    jsonObject[name].asJsonArray.map { gson.fromJson(it, PriorityGroup::class.java) }

                "groups" -> groups = jsonObject[name].asJsonArray.map { gson.fromJson(it, RandomGroup::class.java) }
                "references" -> references =
                    jsonObject[name].asJsonArray.map { it.asString }

                "children" -> children =
                    jsonObject[name].asJsonArray.map { list -> (list.asJsonArray).map { it.asString } }

                "text" -> text = jsonObject[name].asString
                "skipToComponent" -> skipToComponent = jsonObject[name].asString
                "toEnd" -> toEnd = jsonObject[name].asBoolean
                "isActive" -> isActive = jsonObject[name].asBoolean
                "returnType" -> returnType = gson.fromJson(jsonObject[name], ReturnType::class.java)
            }
        }
        return when {
            code.matches(Regex(SKIP_INSTRUCTION_PATTERN)) -> {
                val reservedCode = code.toReservedCode()
                val nonNullableInput = condition ?: (returnType?.defaultTextValue() ?: reservedCode.defaultReturnType()
                    .defaultTextValue())

                SkipInstruction(
                    code = code,
                    skipToComponent = skipToComponent,
                    toEnd = toEnd,
                    condition = nonNullableInput,
                    text = text ?: nonNullableInput,
                    isActive = isActive ?: reservedCode.defaultIsActive(),
                    errors = errors
                )
            }

            code.isReservedCode() -> {
                val reservedCode = code.toReservedCode()
                SimpleState(
                    text = text ?: (returnType?.defaultTextValue() ?: reservedCode.defaultReturnType()
                        .defaultTextValue()),
                    reservedCode,
                    returnType = returnType ?: reservedCode.defaultReturnType(),
                    isActive = isActive ?: reservedCode.defaultIsActive(),
                    errors = errors
                )

            }

            code.matches(Regex(VALID_REFERENCE_INSTRUCTION_PATTERN)) -> {
                Reference(code, references, lang!!, errors)
            }

            code == Instruction.RANDOM_GROUP -> {
                RandomGroups(groups, errors)
            }

            code == Instruction.PRIORITY_GROUPS -> {
                PriorityGroups(priorities, errors)
            }

            code == Instruction.PARENT_RELEVANCE -> {
                ParentRelevance(children, errors)
            }

            else -> throw IllegalArgumentException("Invalid JSON for instruction")
        }
    }

    override fun serialize(value: Instruction, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("code", value.code)
        if (value is State) {
            jsonObject.addProperty("text", value.text)
            jsonObject.add("returnType", gson.toJsonTree(value.returnType))
            jsonObject.addProperty("isActive", value.isActive)
            if (value is SkipInstruction) {
                jsonObject.addProperty("skipToComponent", value.skipToComponent)
                jsonObject.addProperty("condition", value.condition)
                jsonObject.addProperty("toEnd", value.toEnd)
            }
        }
        when (value) {
            is Reference -> {
                jsonObject.add("references", gson.toJsonTree(value.references))
                jsonObject.addProperty("lang", value.lang)
            }

            is RandomGroups -> {
                jsonObject.add("groups", gson.toJsonTree(value.groups))
            }

            is PriorityGroups -> {
                jsonObject.add("priorities", gson.toJsonTree(value.priorities))
            }

            is ParentRelevance -> {
                jsonObject.add("children", gson.toJsonTree(value.children))
            }

            else -> {
                // do nothing
            }
        }
        if (value.errors.isNotEmpty()) {

        }
        return jsonObject
    }
}