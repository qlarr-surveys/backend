package com.frankie.expressionmanager.ext

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.frankie.expressionmanager.model.Dependency
import com.frankie.expressionmanager.model.ImpactMap
import com.frankie.expressionmanager.model.ReservedCode
import com.frankie.expressionmanager.model.SurveyComponent
import com.frankie.expressionmanager.model.SurveyElementType
import com.frankie.expressionmanager.model.childType
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.model.nameAsChildList
import org.json.JSONArray
import org.json.JSONObject


class JsonExt {

    companion object {
        fun addChildren(objectNode: ObjectNode, code: String, state: ObjectNode): ObjectNode =
            objectNode.addChildren(code, state)
    }
}


fun ObjectNode.copyToJSON(
    inCurrentNavigation: Boolean,
    lang: String? = null,
    defaultLang: String
): ObjectNode {
    val returnObj: ObjectNode = JsonNodeFactory.instance.objectNode()
    if (!inCurrentNavigation) {
        listOf("type", "code", "qualifiedCode", "groupType").forEach { key ->
            get(key)?.let {
                returnObj.set<JsonNode>(key, it)
            }
        }
    } else {
        fieldNames().forEach {
            if (it !in listOf(
                    "answers",
                    "groups",
                    "questions",
                    "instructionList",
                    "errors",
                    "content"
                )
            ) {
                returnObj.set<JsonNode>(it, get(it))
            }
        }
    }
    returnObj.set<JsonNode>("inCurrentNavigation", BooleanNode.valueOf(inCurrentNavigation))

    if (has("content")) {
        returnObj.set<JsonNode>("content", get("content"))
        returnObj.reduceContent(lang, defaultLang)
    }

    return returnObj
}

fun ObjectNode.copyReducedToJSON(
    sortedSurveyComponent: SurveyComponent,
    reducedSurveyComponent: SurveyComponent?,
    lang: String? = null,
    defaultLang: String
): ObjectNode {
    val returnObj = copyToJSON(reducedSurveyComponent != null, lang, defaultLang)
    if (reducedSurveyComponent == null && sortedSurveyComponent.elementType == SurveyElementType.QUESTION) {
        return returnObj
    }

    val returnChildren = JsonNodeFactory.instance.arrayNode()

    val children = listOf("answers", "groups", "questions")
        .mapNotNull { childKey ->
            get(childKey) as? ArrayNode
        }
    if (children.size > 1) {
        throw java.lang.IllegalStateException("More than once Child!!!")
    } else if (children.isEmpty()) {
        return returnObj
    }

    val jsonChildren = children[0]


    sortedSurveyComponent.children.forEach { orderedComponent ->
        val childComponent =
            reducedSurveyComponent?.children?.firstOrNull { it.code == orderedComponent.code }
        val childNode = jsonChildren.first {
            (it as ObjectNode).get("code").textValue() == orderedComponent.code
        } as ObjectNode
        returnChildren.add(
            childNode.copyReducedToJSON(
                orderedComponent,
                childComponent,
                lang,
                defaultLang
            )
        )
    }
    if (returnChildren.size() > 0) {
        returnObj.set<JsonNode>(get("code").textValue().childrenName(), returnChildren)
    }

    return returnObj
}

fun ObjectNode.addChildren(code: String, state: ObjectNode): ObjectNode {
    val returnObj = JsonNodeFactory.instance.objectNode()

    (get("children") as? ArrayNode)?.let { childrenNodes ->
        val children = JsonNodeFactory.instance.arrayNode()
        childrenNodes.forEach { child ->
            val childQualifiedCode = (child.get("qualifiedCode") as TextNode).textValue()
            val childCode = (child.get("code") as TextNode).textValue()
            val childObj = state.get(childQualifiedCode) as ObjectNode
            children.add(childObj.addChildren(childCode, state))
        }
        returnObj.set<JsonNode>(code.childrenName(), children)
    }
    remove("children")
    returnObj.set<TextNode>("code", TextNode(code))
    fieldNames().forEach { fieldNAme ->
        returnObj.set<JsonNode>(fieldNAme, get(fieldNAme))
    }
    return returnObj

}

fun ObjectNode.flatten(
    parentCode: String = "",
    returnObj: ObjectNode = JsonNodeFactory.instance.objectNode()
): ObjectNode {
    val code = (get("code") as TextNode).textValue()
    val qualifiedCode = if (code.isUniqueCode()) code else parentCode + code
    val children = listOf("answers", "groups", "questions")
        .mapNotNull { childKey ->
            get(childKey) as? ArrayNode
        }
    if (children.size > 1) {
        throw java.lang.IllegalStateException("More than once Child!!!")
    }
    val childrenNames = JsonNodeFactory.instance.arrayNode()

    children.firstOrNull()?.forEach { child: JsonNode ->
        val childCode = child.get("code").textValue()!!
        val childQualifiedCode =
            if (childCode.isUniqueCode()) childCode else qualifiedCode + childCode
        val childName = JsonNodeFactory.instance.objectNode()
        childName.set<TextNode>("code", child.get("code"))
        if (child.has("type")) {
            childName.set<TextNode>("type", child.get("type"))
        }
        if (child.has("groupType")) {
            childName.set<TextNode>("groupType", child.get("groupType"))
        }
        childName.set<TextNode>("qualifiedCode", TextNode(childQualifiedCode))
        childrenNames.add(childName)
        (child as ObjectNode).flatten(qualifiedCode, returnObj)
    }

    val objectWithoutChildren = JsonNodeFactory.instance.objectNode()
    if (childrenNames.size() > 0) {
        objectWithoutChildren.set<ArrayNode>("children", childrenNames)
    }
    fieldNames().forEach {
        if (it !in listOf("answers", "groups", "questions", "code", "qualifiedCode")) {
            objectWithoutChildren.set<JsonNode>(it, get(it))
        }
    }
    returnObj.set<ObjectNode>(qualifiedCode, objectWithoutChildren)

    return returnObj
}

// only public for testing
internal fun ObjectNode.reduceContent(lang: String? = null, defaultLang: String) {
    val code = lang ?: defaultLang
    (get("content") as? ObjectNode)?.let { content ->
        val fieldsToRemove = mutableListOf<String>()
        content.fieldNames().forEach { fieldName ->
            val contentToBeLocalised = content.get(fieldName)
            if (contentToBeLocalised is ObjectNode && contentToBeLocalised.size() > 0) {
                val node = contentToBeLocalised.get(code) ?: contentToBeLocalised.get(defaultLang)
                content.set<JsonNode>(fieldName, node)
            } else {
                fieldsToRemove.add(fieldName)
            }
        }
        fieldsToRemove.forEach { fieldName ->
            content.remove(fieldName)
        }

    }
    (get("validation") as? ObjectNode)?.let { validation ->
        validation.fieldNames().forEach { validationField ->
            (validation.get(validationField) as? ObjectNode)?.let { validationItem ->
                validationItem.get("content")?.let { contentToBeLocalised ->
                    if (contentToBeLocalised is ObjectNode && contentToBeLocalised.size() > 0) {
                        val node =
                            contentToBeLocalised.get(code) ?: contentToBeLocalised.get(defaultLang)
                        validationItem.set<JsonNode>("content", node)
                        validation.set<JsonNode>(validationField, validationItem)
                    }
                }
            }
        }
    }
}

fun ObjectNode.resources(): List<String> {
    val returnList = mutableListOf<String>()
    (get("resources") as? ObjectNode)?.let { resources ->
        resources.fieldNames().forEach { fieldName ->
            if (resources.get(fieldName) is TextNode) {
                val value = resources.get(fieldName).textValue()
                if (value.isNotBlank()) {
                    returnList.add(value)
                }
            }
        }
    }
    val code = (get("code") as TextNode).textValue()
    (get(code.childrenName()) as? ArrayNode)?.let { childrenNodes ->
        childrenNodes.forEach { child ->
            returnList.addAll((child as ObjectNode).resources())
        }
    }
    return returnList
}

fun ObjectNode.labels(parentCode: String = "", lang: String): Map<String, String> {
    val returnMap = mutableMapOf<String, String>()
    val code = (get("code") as TextNode).textValue()
    val qualifiedCode = if (code.isUniqueCode()) code else parentCode + code
    val label = getLabel(lang, lang)
    returnMap[qualifiedCode] = label
    (get(code.childrenName()) as? ArrayNode)?.let { childrenNodes ->
        childrenNodes.forEach { child ->
            returnMap.putAll((child as ObjectNode).labels(qualifiedCode, lang))
        }
    }
    return returnMap
}

private fun ArrayNode.getByCode(code: String): ObjectNode {
    for (i in 0 until size()) {
        val obj = get(i)
        if (obj.has("code") && obj.get("code").textValue() == code) {
            return obj as ObjectNode
        }
    }
    throw java.lang.IllegalStateException("Child with corresponding code not found")
}

fun SurveyComponent.copyErrorsToJSON(surveyDef: ObjectNode, parentCode: String = ""): ObjectNode {
    if (!surveyDef.has("code") || code != surveyDef.get("code").textValue()) {
        throw IllegalStateException("copyErrorsToJSON: copying into a JsonObject with different code: $code")
    }
    val qualifiedCode = uniqueCode(parentCode)
    val returnObject = surveyDef.deepCopy()
    returnObject.put("qualifiedCode", qualifiedCode)
    if (instructionList.isNotEmpty()) {
        val jsonString = jacksonKtMapper.writeValueAsString(instructionList)
        returnObject.set("instructionList", jacksonKtMapper.readTree(jsonString))
    } else {
        returnObject.remove("instructionList")
    }
    if (errors.isNotEmpty()) {
        val jsonString = jacksonKtMapper.writeValueAsString(errors)
        returnObject.set("errors", jacksonKtMapper.readTree(jsonString))
    } else {
        returnObject.remove("errors")
    }
    val childType = elementType.childType()
    if (children.isNotEmpty()) {
        val newChildren = JsonNodeFactory.instance.arrayNode()
        val jsonChildren = surveyDef.get(childType.nameAsChildList())
        children.filter { it.elementType == childType }.forEachIndexed { index, surveyComponent ->
            val jsonChild = jsonChildren.get(index)
            newChildren.add(
                surveyComponent.copyErrorsToJSON(
                    jsonChild as ObjectNode,
                    qualifiedCode
                )
            )
        }
        returnObject.replace(childType.nameAsChildList(), newChildren)
    }

    return returnObject
}

internal fun ObjectNode.getLabel(lang: String, defaultLang: String): String {
    return (get("content") as? ObjectNode)?.let { content ->
        (content.get("label") as? ObjectNode)?.let { label ->
            if (label.has(lang)) {
                label.get(lang).textValue()
            } else if (label.has(lang)) {
                label.get(defaultLang).textValue()
            } else {
                ""
            }
        }
    } ?: ""
}

internal fun ObjectNode.getChild(codes: List<String>): ObjectNode {
    if (codes.isEmpty())
        return this
    val childrenName = get("code").textValue().childrenName()
    val child = (get(childrenName) as ArrayNode).first { jsonNode ->
        (jsonNode as ObjectNode).get("code").textValue() == codes.first()
    } as ObjectNode
    return child.getChild(codes.toMutableList().drop(1))
}

fun SurveyComponent.getLabels(
    componentJson: ObjectNode,
    parentCode: String,
    lang: String,
    defaultLang: String,
    impactMap: ImpactMap
): Map<Dependency, String> {
    val returnMap = mutableMapOf<Dependency, String>()
    if (!componentJson.has("code") || code != componentJson.get("code").textValue()) {
        throw IllegalStateException("getLabels: copying into a JsonObject with different code: $code")
    }
    val qualifiedCode = uniqueCode(parentCode)
    if (impactMap.keys.contains(Dependency(qualifiedCode, ReservedCode.Label))) {
        returnMap[Dependency(qualifiedCode, ReservedCode.Label)] =
            (componentJson.getLabel(lang, defaultLang))
    }

    if (children.isNotEmpty()) {
        val childType = elementType.childType()
        val jsonChildren = componentJson.get(childType.nameAsChildList()) as ArrayNode
        children.forEach { child ->
            val jsonObject = jsonChildren.getByCode(child.code)
            returnMap.putAll(
                child.getLabels(
                    jsonObject,
                    qualifiedCode,
                    lang,
                    defaultLang,
                    impactMap
                )
            )
        }
    }
    return returnMap
}

fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any> {
    print("jsonObjectToMap: $jsonObject")
    val map: MutableMap<String, Any> = HashMap()
    jsonObject.keys().forEach { key ->
        val value: Any = jsonObject.get(key as String)
        map[key] = jsonValueToObject(value)
    }
    return map
}

fun jsonArrayToList(jsonArray: JSONArray): List<Any> {
    print("jsonArrayToList: $jsonArray")
    val list: MutableList<Any> = ArrayList()
    for (i in 0 until jsonArray.length()) {
        jsonArray.get(i).let { item -> list.add(jsonValueToObject(item)) }
    }
    return list
}

fun jsonValueToObject(jsonValue: Any): Any {
    return when (jsonValue) {
        is JSONObject -> {
            jsonObjectToMap(jsonValue)
        }

        is JSONArray -> {
            jsonArrayToList(jsonValue)
        }

        else -> {
            jsonValue
        }
    }
}

fun valueToJson(value: Any): Any {
    return when (value) {
        is Map<*, *> -> {
            mapToJsonObject(value)
        }

        is List<*> -> {
            listToJsonArray(value)
        }

        else -> {
            value
        }
    }
}

fun mapToJsonObject(map: Map<*, *>): JSONObject {
    val obj = JSONObject()
    map.keys.forEach {
        obj.put(it as String, valueToJson(map[it]!!))
    }
    return obj
}

fun listToJsonArray(list: List<*>): JSONArray {
    val array = JSONArray()
    list.forEach {
        array.put(valueToJson(it!!))
    }
    return array
}