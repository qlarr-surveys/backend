package com.frankie.backend.api.event

import com.fasterxml.jackson.annotation.JsonFormat
import com.frankie.backend.common.DATE_TIME_UTC_FORMAT
import java.time.LocalDateTime

data class EventDTO(
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val time: LocalDateTime,
    val name: String,
    val details: String?,
)
