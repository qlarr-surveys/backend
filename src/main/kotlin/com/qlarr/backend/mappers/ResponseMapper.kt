package com.qlarr.backend.mappers

import com.qlarr.backend.api.response.ResponseDto
import com.qlarr.backend.common.utcToLocalTZ
import com.qlarr.backend.services.ResponseWithSurveyorName
import com.qlarr.surveyengine.model.ReservedCode
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class ResponseMapper {

    fun toDto(
        entity: ResponseWithSurveyorName,
        valueNames: List<String>,
        maskedValues: Map<String, Any> = mapOf(),
        clientZoneId: ZoneId? = null
    ) =
        ResponseDto(
            id = entity.response.id!!,
            index = entity.response.surveyResponseIndex,
            startDate = entity.response.startDate.utcToLocalTZ(clientZoneId),
            surveyorID = entity.response.surveyor?.toString(),
            surveyorName = if (entity.response.surveyor != null) "${entity.firstName} ${entity.lastName}" else null,
            submitDate = entity.response.submitDate?.utcToLocalTZ(clientZoneId),
            lang = entity.response.lang,
            preview = entity.response.preview,
            version = entity.response.version,
            values = LinkedHashMap(valueNames.map { valueName ->
                val names = valueName.split(".")
                if (names[1] == "value") {
                    valueName to (maskedValues["${names[0]}.${ReservedCode.MaskedValue.code}"]?.let {
                        "$it [${entity.response.values[valueName]}]"
                    } ?: entity.response.values[valueName])
                } else {
                    valueName to entity.response.values[valueName]
                }
            }.associate { it.first to it.second })
        )
}

fun List<Map<String, Any>>.valueNames(): List<String> = mutableListOf<String>().apply {
    this@valueNames.forEach {
        addAll(it.keys.toList())
    }
}.distinct().filter {
    it.split(".")[1] == "value"
}