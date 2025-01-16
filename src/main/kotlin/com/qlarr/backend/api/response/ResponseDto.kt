package com.qlarr.backend.api.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import java.time.LocalDateTime
import java.util.*

data class ResponseDto(
        val id: UUID,

        val index : Int?,

        @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
        val startDate: LocalDateTime,

        @JsonFormat(pattern = DATE_TIME_UTC_FORMAT)
        val submitDate: LocalDateTime? = null,

        val lang: String,

        val values: LinkedHashMap<String,Any?> = linkedMapOf(),

        val preview: Boolean,

        val surveyorName: String?,

        val surveyorID: String?,

        val version: Int
)

data class ResponsesDto(
        val totalCount: Int,
        val totalPages: Int,
        val pageNumber: Int,
        val columnNames: List<String> = listOf(),
        val responses: List<ResponseDto> = listOf()
)

data class ResponseCountDto(
        val completeResponseCount: Int,
        val userResponsesCount: Int
)
