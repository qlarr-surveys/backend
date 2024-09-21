package com.qlarr.backend.expressionmanager

import com.qlarr.backend.exceptions.WrongColumnException
import com.qlarr.backend.exceptions.WrongValueType
import com.qlarr.expressionmanager.model.DataType
import com.qlarr.expressionmanager.model.ResponseField
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

fun validateType(value: Any, dataType: DataType): Boolean {
    return when (dataType) {
        DataType.BOOLEAN -> value is Boolean
        DataType.DATE -> value is String
        DataType.STRING -> value is String
        DataType.DOUBLE -> value is Number
        DataType.INT -> value is Int
        DataType.LIST -> value is JSONArray
        DataType.FILE,
        DataType.MAP -> value is Map<*,*>
    }
}

fun expectedType(dataType: DataType): String {
    return when (dataType) {
        DataType.BOOLEAN -> Boolean::class.java.name
        DataType.DATE -> String::class.java.name
        DataType.STRING -> String::class.java.name
        DataType.DOUBLE -> Number::class.java.name
        DataType.INT -> Int::class.java.name
        DataType.LIST -> JSONArray::class.java.name
        DataType.FILE,
        DataType.MAP -> JSONObject::class.java.name
    }
}
