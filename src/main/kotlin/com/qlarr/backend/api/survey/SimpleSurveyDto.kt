package com.qlarr.backend.api.survey

import com.fasterxml.jackson.annotation.JsonFormat
import com.qlarr.backend.api.version.VersionDto
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import com.qlarr.backend.persistence.entities.SurveyNavigationData
import java.time.LocalDateTime
import java.util.*

data class SimpleSurveyDto(
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
    val description: String?,
    val image: String?,
    val status: Status,
    val usage: Usage,
    val surveyQuota: Int,
    val responsesCount: Int,
    val completeResponseCount: Int,
    val latestVersion: VersionDto,
    val navigationData: SurveyNavigationData = SurveyNavigationData(),
)


data class SurveysDto(
        val totalCount: Int,
        val totalPages: Int,
        val pageNumber: Int,
        val surveys: List<SimpleSurveyDto> = listOf()
)

data class OfflineSurveyDto(
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
    val description: String?,
    val image: String?,
    val status: Status,
    val usage: Usage,
    val surveyQuota: Int,
    val userResponsesCount: Int,
    val completeResponseCount: Int,
    val latestVersion: VersionDto,
    val navigationData: SurveyNavigationData = SurveyNavigationData(),
)
