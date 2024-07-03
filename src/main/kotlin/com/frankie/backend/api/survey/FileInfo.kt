package com.frankie.backend.api.survey

import com.fasterxml.jackson.annotation.JsonFormat
import com.frankie.backend.common.DATE_TIME_UTC_FORMAT
import java.time.LocalDateTime

data class FileInfo(
    val name: String,
    val size: Long,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val lastModified: LocalDateTime
)
