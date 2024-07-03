package com.frankie.expressionmanager.model.adapters

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.frankie.expressionmanager.ext.VALID_REFERENCE_INSTRUCTION_PATTERN
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.*


class InstructionSerializer : StdSerializer<Instruction>(Instruction::class.java) {
    override fun serialize(value: Instruction, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("code", value.code)
        if (value is State) {
            gen.writeStringField("text", value.text)
            gen.writeObjectField("returnType", value.returnType)
            gen.writeBooleanField("isActive", value.isActive)
            if (value is SkipInstruction) {
                gen.writeStringField("skipToComponent", value.skipToComponent)
                gen.writeStringField("condition", value.condition)
                gen.writeBooleanField("toEnd", value.toEnd)
            }
        }
        when (value) {
            is Reference -> {
                gen.writeArrayFieldStart("references")
                value.references.forEach {
                    gen.writeString(it)
                }
                gen.writeEndArray()
                gen.writeStringField("lang", value.lang)
            }

            is RandomGroups -> {
                gen.writeArrayFieldStart("groups")
                value.groups.forEach { randomGroup ->
                    gen.writeObject(randomGroup)
                }
                gen.writeEndArray()
            }

            is PriorityGroups -> {
                gen.writeArrayFieldStart("priorities")
                value.priorities.forEach {
                    gen.writeObject(it)
                }
                gen.writeEndArray()
            }

            is ParentRelevance -> {
                gen.writeArrayFieldStart("children")
                value.children.forEach { list ->
                    gen.writeStartArray()
                    list.forEach {
                        gen.writeString(it)
                    }
                    gen.writeEndArray()
                }
                gen.writeEndArray()
            }

            else -> {
                // do nothing
            }
        }
        if (value.errors.isNotEmpty()) {
            gen.writeArrayFieldStart("errors")
            value.errors.forEach {
                gen.writeObject(it)
            }
            gen.writeEndArray()
        }
        gen.writeEndObject()
    }
}

class InstructionDeserializer : StdDeserializer<Instruction>(Instruction::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instruction {
        val node: JsonNode = p.codec.readTree(p)
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
        node.fieldNames().forEach { name ->
            when (name) {
                "code" -> code = node[name].textValue()
                "lang" -> lang = node[name].textValue()
                "condition" -> condition = node[name].textValue()
                "errors" -> errors = (node[name] as ArrayNode).map { jacksonKtMapper.treeToValue(it) }
                "priorities" -> priorities =
                    (node[name] as ArrayNode).map { jacksonKtMapper.treeToValue(it) }

                "groups" -> groups = (node[name] as ArrayNode).map { jacksonKtMapper.treeToValue(it) }
                "references" -> references =
                    (node[name] as ArrayNode).map { it.textValue() }

                "children" -> children =
                    (node[name] as ArrayNode).map { list -> (list as ArrayNode).map { it.textValue() } }

                "text" -> text = node[name].textValue()
                "skipToComponent" -> skipToComponent = node[name].textValue()
                "toEnd" -> toEnd = node[name].booleanValue()
                "isActive" -> isActive = node[name].booleanValue()
                "returnType" -> returnType =
                    jacksonKtMapper.treeToValue(node[name] as ObjectNode, ReturnType::class.java)
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

}
