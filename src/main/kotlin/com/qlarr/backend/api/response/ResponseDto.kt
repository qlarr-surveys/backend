package com.qlarr.backend.api.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import com.qlarr.backend.persistence.entities.ResponseSummaryInterface
import java.time.LocalDateTime
import java.util.*

data class ResponseDto(
    val id: UUID,

    val index: Int?,

    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val startDate: LocalDateTime,

    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val submitDate: LocalDateTime? = null,

    val lang: String,

    val preview: Boolean,

    val disqualified: Boolean,

    val values: List<ResponseValue> = emptyList(),

    val surveyorName: String?,

    val surveyorID: String?
)

data class ResponseValue(
    val key: String,
    val code: String,
    val value: Any? = null,
)


data class ResponsesDto(
    val totalCount: Int,
    val totalPages: Int,
    val pageNumber: Int,
    val columnNames: List<String> = listOf(),
    val responses: List<ResponseDto> = listOf()
)

data class ResponsesSummaryDto(
    val totalCount: Int,
    val totalPages: Int,
    val pageNumber: Int,
    val responses: List<ResponseSummary> = listOf(),
    val canExportFiles: Boolean
)

data class ResponseCountDto(
    val completeResponseCount: Int,
    val userResponsesCount: Int
)

fun toData(summary: ResponseSummaryInterface) = ResponseSummary(
    summary.id,
    summary.index,
    summary.surveyId,
    summary.surveyor,
    summary.startDate,
    summary.submitDate,
    summary.lang,
    summary.preview,
    summary.disqualified,
    summary.firstName,
    summary.lastName
)

data class ResponseSummary(
    val id: UUID,
    val index: Long,
    val surveyId: UUID,
    val surveyor: UUID?,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val startDate: LocalDateTime,
    @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
    val submitDate: LocalDateTime?,
    val lang: String,
    val preview: Boolean,
    val disqualified: Boolean?,
    val firstName: String?,
    val lastName: String?
)
