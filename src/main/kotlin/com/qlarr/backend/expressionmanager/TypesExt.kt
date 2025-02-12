package com.qlarr.backend.expressionmanager

import com.qlarr.backend.exceptions.WrongColumnException
import com.qlarr.backend.exceptions.WrongValueType
import com.qlarr.surveyengine.model.ResponseField
import com.qlarr.surveyengine.model.ReturnType
import org.json.JSONArray
import org.json.JSONObject


fun Map<String, Any>.validateSchema(responsesSchema: List<ResponseField>) {
    forEach { entry ->
        val responseField = responsesSchema.firstOrNull { it.toValueKey() == entry.key }
        if (responseField == null) {
            throw WrongColumnException(entry.key)
        } else if (!validateType(entry.value, responseField.dataType)) {
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
        ReturnType.BOOLEAN -> value is Boolean
        ReturnType.DATE -> value is String
        ReturnType.STRING -> value is String
        ReturnType.DOUBLE -> value is Number
        ReturnType.INT -> value is Int
        ReturnType.LIST -> value is JSONArray
        ReturnType.FILE,
        ReturnType.MAP -> value is Map<*,*>
    }
}

fun expectedType(dataType: ReturnType): String {
    return when (dataType) {
        ReturnType.BOOLEAN -> Boolean::class.java.name
        ReturnType.DATE -> String::class.java.name
        ReturnType.STRING -> String::class.java.name
        ReturnType.DOUBLE -> Number::class.java.name
        ReturnType.INT -> Int::class.java.name
        ReturnType.LIST -> JSONArray::class.java.name
        ReturnType.FILE,
        ReturnType.MAP -> JSONObject::class.java.name
    }
}
