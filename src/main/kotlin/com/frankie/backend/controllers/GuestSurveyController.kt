package com.frankie.backend.controllers

import com.frankie.backend.api.survey.CloneRequest
import com.frankie.backend.api.survey.SimpleSurveyDto
import com.frankie.backend.api.survey.SurveyDTO
import com.frankie.backend.services.SurveyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class GuestSurveyController(private val surveyService: SurveyService) {

    @GetMapping("/guest/survey/all")
    fun getAllForGuest(): ResponseEntity<List<SimpleSurveyDto>> {
        val surveyDTOList = surveyService.getAllSurveysForGuest()
        return ResponseEntity(surveyDTOList, HttpStatus.OK)
    }

    @PostMapping("/guest/survey/{surveyId}/clone")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun cloneGuestSurvey(
        @PathVariable surveyId: UUID,
        @RequestBody cloneRequest: CloneRequest
    ): ResponseEntity<SurveyDTO> {
        val surveyDTO = surveyService.cloneGuestSurvey(surveyId, cloneRequest.name)
        return ResponseEntity(surveyDTO, HttpStatus.OK)
    }

}