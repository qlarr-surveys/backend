package com.frankie.backend.mappers

import com.frankie.backend.api.response.ResponseDto
import com.frankie.backend.common.utcToLocalTZ
import com.frankie.backend.services.ResponseWithSurveyorName
import com.frankie.expressionmanager.model.Dependency
import com.frankie.expressionmanager.model.ReservedCode
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class ResponseMapper {

    fun toDto(
            entity: ResponseWithSurveyorName,
            valueNames: List<String>,
            maskedValues: Map<Dependency, Any> = mapOf(),
            clientZoneId: ZoneId? = null
    ) =
            ResponseDto(
                    id = entity.response.id!!,
                    startDate = entity.response.startDate.utcToLocalTZ(clientZoneId),
                    surveyorID = entity.response.surveyor?.toString(),
                    surveyorName = if (entity.response.surveyor != null) "${entity.firstName} ${entity.lastName}" else null,
                    submitDate = entity.response.submitDate?.utcToLocalTZ(clientZoneId),
                    lang = entity.response.lang,
                    preview = entity.response.preview,
                    version = entity.response.version,
                    values = valueNames.map { valueName ->
                        val names = valueName.split(".")
                        if (entity.response.values[valueName] == null || entity.response.values[valueName] == "") {
                            null
                        } else if (names[1] == "value") {
                            maskedValues[Dependency(names[0], ReservedCode.MaskedValue)]?.let {
                                "$it [${entity.response.values[valueName]}]"
                            } ?: entity.response.values[valueName]
                        } else {
                            entity.response.values[valueName]
                        }
                    }
            )
}

fun List<Map<String, Any>>.valueNames(): List<String> = mutableListOf<String>().apply {
    this@valueNames.forEach {
        addAll(it.keys.toList())
    }
}.distinct()