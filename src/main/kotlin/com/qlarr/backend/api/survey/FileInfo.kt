package com.qlarr.backend.api.survey

import com.fasterxml.jackson.annotation.JsonFormat
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import java.time.LocalDateTime

data class FileInfo(
    val name: String,
    val size: Long,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val lastModified: LocalDateTime
)

data class AutoCompleteFileInfo(
    val name: String,
    val rowCount:Int,
    val size: Long,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val lastModified: LocalDateTime
)
