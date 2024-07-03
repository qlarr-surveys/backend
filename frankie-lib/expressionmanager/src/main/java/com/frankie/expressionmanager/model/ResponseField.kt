package com.frankie.expressionmanager.model


data class ResponseField(
    val componentCode: String,
    val columnName: ColumnName,
    val dataType: DataType
) {
    fun toValueKey() = "$componentCode.${columnName.name.lowercase()}"
}

enum class DataType {
    BOOLEAN, STRING, INT, DOUBLE, LIST, MAP, DATE, FILE
}

enum class ColumnName {
    VALUE,
    ORDER,
    PRIORITY;
}

data class ResponseLabel(
    val type: String,
    val label: String
)