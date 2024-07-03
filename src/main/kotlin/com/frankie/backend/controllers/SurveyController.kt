package com.frankie.backend.controllers

import com.frankie.backend.api.survey.*
import com.frankie.backend.common.UserUtils
import com.frankie.backend.services.SurveyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class SurveyController(
        private val surveyService: SurveyService,
        private val userUtils: UserUtils,
) {

    @GetMapping("/survey/all")
    fun getAll(
            @RequestParam page: Int?,
            @RequestParam("per_page") perPage: Int?,
            @RequestParam("sort_by") sortBy: String?,
            @RequestParam status: String?
    ): ResponseEntity<SurveysDto> {
        val surveyDTOList = surveyService.getAllSurveys(
                page, perPage, sortBy, status
        )
        return ResponseEntity(surveyDTOList, HttpStatus.OK)
    }

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
    fun edit(@PathVariable surveyId: UUID, @RequestBody editSurveyRequest: EditSurveyRequest): ResponseEntity<SurveyDTO> {
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
    fun cloneSurvey(@PathVariable surveyId: UUID, @RequestBody cloneRequest: CloneRequest): ResponseEntity<SurveyDTO> {
        val surveyDTO = surveyService.clone(surveyId, cloneRequest.name)
        return ResponseEntity(surveyDTO, HttpStatus.OK)
    }
}
