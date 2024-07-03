package com.frankie.backend.api.survey

import com.fasterxml.jackson.annotation.JsonFormat
import com.frankie.backend.common.DATE_TIME_UTC_FORMAT
import com.frankie.backend.common.nowUtc
import com.frankie.expressionmanager.model.NavigationMode
import com.frankie.expressionmanager.model.SurveyLang
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
