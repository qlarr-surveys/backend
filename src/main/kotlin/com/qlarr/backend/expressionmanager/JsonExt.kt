package com.qlarr.backend.expressionmanager

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.surveyengine.ext.childrenName

private fun ObjectNode.changeRelevanceObject(from: String, to: String) {
    (get("relevance") as? ObjectNode)?.toString()?.replace(from, to)?.let {
        this@changeRelevanceObject.set<JsonNode>("relevance", objectMapper.readTree(it))
    }
}

private fun ObjectNode.changeSkipLogicObject(from: String, to: String) {
    (get("skip_logic") as? ObjectNode)?.toString()?.replace(from, to)?.let {
        this@changeSkipLogicObject.set<JsonNode>("skip_logic", objectMapper.readTree(it))
    }
}

fun ObjectNode.changeSkipLogicObject(path: List<String>, from: String, to: String) {
    if (path.isEmpty()) {
        changeSkipLogicObject(from, to)
        return
    }

    val childrenName = this["code"].textValue().childrenName()
    val childrenArray = this[childrenName] as? ArrayNode
        ?: throw IllegalStateException("No children found")
    val targetChildCode = path.first()
    (childrenArray.firstOrNull {
        it["code"]?.textValue() == targetChildCode
    } as? ObjectNode)?.changeSkipLogicObject(path.drop(1), from, to)
}

fun ObjectNode.changeRelevanceObject(path: List<String>, from: String, to: String) {
    if (path.isEmpty()) {
        changeRelevanceObject(from, to)
        return
    }

    val childrenName = this["code"].textValue().childrenName()
    val childrenArray = this[childrenName] as? ArrayNode
        ?: throw IllegalStateException("No children found")
    val targetChildCode = path.first()
    (childrenArray.firstOrNull {
        it["code"]?.textValue() == targetChildCode
    } as? ObjectNode)?.changeRelevanceObject(path.drop(1), from, to)
}