package com.qlarr.backend.api.survey

import com.fasterxml.jackson.annotation.JsonFormat
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import com.qlarr.backend.common.nowUtc
import java.time.LocalDateTime
import java.util.*

data class SurveyDTO(
    val id: UUID,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val creationDate: LocalDateTime,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val lastModified: LocalDateTime,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val startDate: LocalDateTime?,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val endDate: LocalDateTime?,
    val image: String?,
    val description: String?,
    val name: String,
    val status: Status,
    val usage: Usage,
    val quota: Int,
    val canLockSurvey: Boolean,
){
    fun isActive(): Boolean {
        return status == Status.ACTIVE
                && (endDate == null || endDate.isAfter(nowUtc()))
                && (startDate == null || startDate.isBefore(nowUtc()))
    }
}
