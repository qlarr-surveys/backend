package com.qlarr.backend.api.offline

import com.fasterxml.jackson.annotation.JsonFormat
import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import com.qlarr.surveyengine.usecase.ValidationJsonOutput
import java.time.LocalDateTime

data class DesignDiffDto(
    val files: List<FileInfo> = listOf(),
    val publishInfo: PublishInfo,
    val validationJsonOutput: ValidationJsonOutput? = null
)

data class PublishInfo(
    val version: Int,
    val subVersion: Int,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val lastModified: LocalDateTime,
)



