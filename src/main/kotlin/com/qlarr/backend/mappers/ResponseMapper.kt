package com.qlarr.backend.mappers

import com.qlarr.backend.api.response.ResponseDto
import com.qlarr.backend.api.response.ResponseValue
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import org.springframework.stereotype.Component

@Component
class ResponseMapper {


    fun toDto(
        disqualified: Boolean,
        entity: SurveyResponseEntity,
        values: List<ResponseValue> = emptyList(),
    ) =
        ResponseDto(
            id = entity.id,
            index = entity.surveyResponseIndex,
            startDate = entity.startDate,
            surveyorID = entity.surveyor?.toString(),
            surveyorName = null,
            preview = entity.preview,
            submitDate = entity.submitDate,
            lang = entity.lang,
            disqualified = disqualified,
            values = values
        )
}

fun List<Map<String, Any>>.valueNames(): List<String> = mutableListOf<String>().apply {
    this@valueNames.forEach {
        addAll(it.keys.toList())
    }
}.distinct().filter {
    it.split(".")[1] == "value"
}