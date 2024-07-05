package com.frankie.backend.api.survey

import com.fasterxml.jackson.annotation.JsonFormat
import com.frankie.backend.common.DATE_TIME_UTC_FORMAT
import com.frankie.expressionmanager.model.NavigationMode
import com.frankie.expressionmanager.model.SurveyLang
import java.time.LocalDateTime

data class SurveyCreateRequest(
    val name: String,
    val usage: Usage = Usage.MIXED
)

data class EditSurveyRequest(
    val name: String? = null,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val startDate: LocalDateTime? = null,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val endDate: LocalDateTime? = null,
    val additionalLanguages: List<SurveyLang>? = null,
    val usage: Usage? = null,

    val backgroundAudio: Boolean? = null,
    val recordGps: Boolean? = null,
    val canLockSurvey: Boolean? = null,

    val quota: Int? = null,
    val publicWithinOrg: Boolean? = null,

    val saveIp: Boolean? = null,
    val saveTimings: Boolean? = null,
)

data class CloneRequest(val name: String)
