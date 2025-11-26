package com.qlarr.backend.api.survey

import com.fasterxml.jackson.annotation.JsonFormat
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import com.qlarr.backend.persistence.entities.SurveyNavigationData
import com.qlarr.backend.persistence.entities.TEN_YEARS_MILLIS
import com.qlarr.surveyengine.model.SurveyLang
import com.qlarr.surveyengine.model.exposed.NavigationMode
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
    val canLockSurvey: Boolean? = null,
    val quota: Int? = null,
    val description: String? = null,
    val image: String? = null,
    val navigationMode: NavigationMode? = null,
    val allowPrevious: Boolean? = null,
    val resumeExpiryMillis: Long? = null,
    val skipInvalid: Boolean? = null,
    val allowIncomplete: Boolean? = null,
    val responseReviewRequired: Boolean? = null,
    val allowJump: Boolean? = null,
)