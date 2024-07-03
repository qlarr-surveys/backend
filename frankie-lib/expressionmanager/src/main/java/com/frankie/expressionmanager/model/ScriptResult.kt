package com.frankie.expressionmanager.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class ScriptResult(val resultType: ResultType, private val valueOrError: Any) {
    @JsonIgnore
    fun isError() = resultType != ResultType.Value
    @JsonIgnore
    fun getValue() = if (!isError()) valueOrError else null
}

enum class ResultType {
    Value, SYNTAX_ERROR, INPUT_FORMAT_ERROR, NULL_RETURN
}

