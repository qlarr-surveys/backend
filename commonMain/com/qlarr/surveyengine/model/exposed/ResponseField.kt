package com.qlarr.surveyengine.model.exposed

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport


@OptIn(ExperimentalJsExport::class)
@Serializable
@JsExport
data class ResponseField(
    val componentCode: String,
    val columnName: ColumnName,
    val dataType: ReturnType
) {
    @Suppress("unused")
    fun toValueKey() = "$componentCode.${columnName.name.lowercase()}"
}

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class ColumnName {
    VALUE,
    ORDER,
    PRIORITY;
}