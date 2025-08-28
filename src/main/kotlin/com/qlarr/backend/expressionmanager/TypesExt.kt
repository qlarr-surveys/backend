package com.qlarr.backend.expressionmanager

import com.qlarr.backend.exceptions.WrongValueType
import com.qlarr.surveyengine.model.exposed.ResponseField
import com.qlarr.surveyengine.model.exposed.ReturnType
import org.json.JSONArray
import org.json.JSONObject


fun Map<String, Any>.validateSchema(responsesSchema: List<ResponseField>) {
    val dependents = responsesSchema.map { it.toValueKey() }
    val valuesMap = filterKeys { key->
        dependents.contains(key)
    }
    valuesMap.forEach { entry ->
        val responseField = responsesSchema.first { it.toValueKey() == entry.key }
        if (!validateType(entry.value, responseField.dataType)) {
            throw WrongValueType(
                columnName = entry.key,
                expectedClassName = expectedType(responseField.dataType),
                actualClassName = entry.value.javaClass.name
            )
        }
    }
}

fun validateType(value: Any, dataType: ReturnType): Boolean {
    return when (dataType) {
        ReturnType.Boolean -> value is Boolean
        ReturnType.Date -> value is String
        is ReturnType.Enum,
        ReturnType.String -> value is String
        ReturnType.Double -> value is Number
        ReturnType.Int -> value is Int
        ReturnType.List -> value is JSONArray || value is List<*>
        ReturnType.File,
        ReturnType.Map -> value is Map<*,*>
    }
}

fun expectedType(dataType: ReturnType): String {
    return when (dataType) {
        ReturnType.Boolean -> Boolean::class.java.name
        ReturnType.Date -> String::class.java.name
        is ReturnType.Enum,
        ReturnType.String -> String::class.java.name
        ReturnType.Double -> Number::class.java.name
        ReturnType.Int -> Int::class.java.name
        ReturnType.List -> JSONArray::class.java.name
        ReturnType.File,
        ReturnType.Map -> JSONObject::class.java.name
    }
}
