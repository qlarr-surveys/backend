package com.qlarr.backend.controllers

import com.qlarr.backend.api.response.ResponseDto
import com.qlarr.backend.api.response.ResponseFormat
import com.qlarr.backend.api.response.ResponseStatus
import com.qlarr.backend.api.response.ResponsesSummaryDto
import com.qlarr.backend.exceptions.UnrecognizedZoneException
import com.qlarr.backend.services.ResponseService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId
import java.util.*

@RestController
class ResponseController(
    private val responseService: ResponseService
) {
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','analyst'})")
    @GetMapping("/response/{responseId}")
    fun getResponses(
        @PathVariable responseId: UUID,
    ): ResponseEntity<ResponseDto> {
        val result = responseService.getResponse(
            responseId,
        )
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','analyst'})")
    @GetMapping("/survey/{surveyId}/response/summary")
    fun getAllResponses(
        @PathVariable surveyId: UUID,
        @RequestParam page: Int?,
        @RequestParam("per_page") perPage: Int?,
        @RequestParam surveyor: UUID?,
        @RequestParam status: String?
    ): ResponseEntity<ResponsesSummaryDto> {
        val result = responseService.getSummary(
            surveyId,
            page,
            perPage,
            ResponseStatus.fromString(status),
            surveyor,
        )
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','analyst'})")
    @GetMapping("/survey/{surveyId}/response/export/{format}/{from}/{to}")
    fun exportResponses(
        @PathVariable surveyId: UUID,
        @PathVariable("format") format: String,
        @PathVariable("from") from: Int,
        @PathVariable("to") to: Int,
        @RequestParam("db_values") dbValues: Boolean?,
        @RequestParam complete: Boolean?,
        @RequestParam timezone: String
    ): ResponseEntity<ByteArray> {
        val clientZoneId = try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            throw UnrecognizedZoneException(timezone)
        }

        val responseFormat = ResponseFormat.fromString(format)

        val result = if (dbValues != false)
            responseService.exportResponses(surveyId, complete, clientZoneId, responseFormat, from, to)
        else
            responseService.exportTextResponses(surveyId, complete, clientZoneId, responseFormat, from, to)
        return ResponseEntity.ok()
            .header(CONTENT_TYPE, responseFormat.contentType())
            .header(
                "Content-Disposition",
                "attachment; filename=\"$surveyId-responses-export.${responseFormat.name.lowercase()}\""
            )
            .body(result)
    }


    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','analyst'})")
    @GetMapping("/survey/{surveyId}/response/files/download/{from}/{to}")
    fun bulkDownloadResponseFiles(
        @PathVariable surveyId: UUID,
        @PathVariable("from") from: Int,
        @PathVariable("to") to: Int,
        @RequestParam complete: Boolean?,
    ): ResponseEntity<InputStreamResource> {
        return responseService.bulkDownloadResponses(surveyId, complete, from, to)
    }
}
