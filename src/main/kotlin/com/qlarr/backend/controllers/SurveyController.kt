package com.qlarr.backend.controllers

import com.qlarr.backend.api.survey.EditSurveyRequest
import com.qlarr.backend.api.survey.SurveyCreateRequest
import com.qlarr.backend.api.survey.SurveyDTO
import com.qlarr.backend.services.SurveyService
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
class SurveyController(private val surveyService: SurveyService) {


    @PostMapping("/survey/create")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun create(@RequestBody surveyCreateRequest: SurveyCreateRequest): ResponseEntity<SurveyDTO> {
        val surveyDTO = surveyService.create(surveyCreateRequest)
        return ResponseEntity(surveyDTO, HttpStatus.OK)
    }

    @GetMapping("/survey/{surveyId}")
    fun getById(@PathVariable surveyId: UUID): ResponseEntity<SurveyDTO> {
        val surveyDTO = surveyService.getSurveyById(surveyId)
        return ResponseEntity(surveyDTO, HttpStatus.OK)
    }


    @PutMapping("/survey/{surveyId}")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun edit(
        @PathVariable surveyId: UUID,
        @RequestBody editSurveyRequest: EditSurveyRequest
    ): ResponseEntity<SurveyDTO> {
        val surveyDtoResponse = surveyService.edit(surveyId, editSurveyRequest)
        return ResponseEntity(surveyDtoResponse, HttpStatus.OK)
    }

    @PutMapping("/survey/{surveyId}/close")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun close(@PathVariable surveyId: UUID): ResponseEntity<SurveyDTO> {
        val surveyDtoResponse = surveyService.close(surveyId)
        return ResponseEntity(surveyDtoResponse, HttpStatus.OK)
    }


    @DeleteMapping("/survey/{surveyId}")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun delete(@PathVariable surveyId: UUID): ResponseEntity<Any> {
        surveyService.delete(surveyId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @PostMapping("/survey/{surveyId}/clone")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun cloneSurvey(@PathVariable surveyId: UUID): ResponseEntity<SurveyDTO> {
        val surveyDTO = surveyService.clone(surveyId)
        return ResponseEntity(surveyDTO, HttpStatus.OK)
    }

    @GetMapping("/survey/{surveyId}/export")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun exportSurvey(@PathVariable surveyId: UUID): ResponseEntity<ByteArray> {
        return ResponseEntity.ok()
            .header(CONTENT_TYPE, "application/zip")
            .header(
                "Content-Disposition",
                "inline; filename=\"$surveyId.zip\""
            )
            .body(surveyService.exportSurvey(surveyId))
    }

    @PostMapping("/survey/import")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    fun importSurvey(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<SurveyDTO> {
        return ResponseEntity(surveyService.importSurvey(file.inputStream), HttpStatus.OK)
    }
}
