package com.frankie.backend.api.version

import com.fasterxml.jackson.annotation.JsonFormat
import com.frankie.backend.api.survey.Status
import com.frankie.backend.common.DATE_TIME_UTC_FORMAT
import java.time.LocalDateTime
import java.util.*

data class VersionDto(
    val surveyId: UUID,
    val version: Int,
    val subVersion: Int,
    val valid: Boolean,
    val published: Boolean,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val lastModified: LocalDateTime? = null,
    val status:Status
)