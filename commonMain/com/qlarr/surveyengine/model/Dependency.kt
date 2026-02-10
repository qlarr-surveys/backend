package com.qlarr.surveyengine.model

import com.qlarr.surveyengine.model.exposed.ColumnName
import com.qlarr.surveyengine.model.exposed.ResponseField
import kotlinx.serialization.Serializable

@Serializable
data class Dependency(val componentCode: String, val reservedCode: ReservedCode) : Comparable<Dependency> {
    override fun compareTo(other: Dependency): Int {
        return "$componentCode.$reservedCode".compareTo("${other.componentCode}.${other.reservedCode}")
    }

    fun asCode(): String {
        return "$componentCode.${reservedCode.code}"
    }

    fun toValueKey() = "$componentCode.${reservedCode.code}"

}

val langDependency = Dependency("Survey", ReservedCode.Lang)
val modeDependency = Dependency("Survey", ReservedCode.Mode)

fun String.toDependency(): Dependency? = split(".", ignoreCase = true).let {
    if (it[1].isReservedCode()) Dependency(
        it[0],
        it[1].toReservedCode()
    ) else null
}

fun String.toDependent(): Dependent = split(".").let { Dependent(it[0], it[1]) }


data class Dependent(val componentCode: String, val instructionCode: String) : Comparable<Dependent> {
    override fun compareTo(other: Dependent): Int {
        return "$componentCode.$instructionCode".compareTo("${other.componentCode}.${other.instructionCode}")
    }

    fun toValueKey() = "$componentCode.$instructionCode"
}

fun ResponseField.toDependency() = Dependency(componentCode, columnName.toReservedCode())
fun ResponseField.toDependent() = Dependent(componentCode, columnName.name.lowercase())

fun ColumnName.toReservedCode(): ReservedCode = when (this) {
    ColumnName.VALUE -> ReservedCode.Value
    ColumnName.ORDER -> ReservedCode.Order
    ColumnName.PRIORITY -> ReservedCode.Priority
}