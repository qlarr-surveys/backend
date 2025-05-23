package com.qlarr.backend.controllers

import com.qlarr.backend.api.response.ResponsesDto
import com.qlarr.backend.api.user.*
import com.qlarr.backend.exceptions.UnrecognizedZoneException
import com.qlarr.backend.services.ResponseService
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.ZoneId
import java.util.*

@RestController
class ResponseController(
        private val responseService: ResponseService
) {
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','analyst'})")
    @GetMapping("/survey/{surveyId}/response/all")
    fun getAllResponses(
            @PathVariable surveyId: UUID,
            @RequestParam page: Int?,
            @RequestParam("per_page") perPage: Int?,
            @RequestParam("db_values") dbValues: Boolean?,
            @RequestParam surveyor: UUID?,
            @RequestParam complete: Boolean?
    ): ResponseEntity<ResponsesDto> {
        val result = if (dbValues != false) responseService.getAllResponses(
                surveyId,
                page,
                perPage,
                complete,
                surveyor
        ) else responseService.getAllTextResponses(
                surveyId, page, perPage, complete,
                surveyor
        )
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','analyst'})")
    @GetMapping("/survey/{surveyId}/response/export")
    fun exportResponses(
            @PathVariable surveyId: UUID,
            @RequestParam("db_values") dbValues: Boolean?,
            @RequestParam complete: Boolean?,
            @RequestParam timezone: String
    ): ResponseEntity<ByteArray> {

        val clientZoneId = try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            throw UnrecognizedZoneException(timezone)
        }

        val result = if (dbValues != false)
            responseService.exportResponses(surveyId, complete, clientZoneId)
        else
            responseService.exportTextResponses(surveyId, complete, clientZoneId)
        return ResponseEntity.ok()
                .header(CONTENT_TYPE, "text/csv")
                .header("Content-Disposition", "attachment; filename=\"$surveyId-responses-export.csv\"")
                .body(result)
    }
}
