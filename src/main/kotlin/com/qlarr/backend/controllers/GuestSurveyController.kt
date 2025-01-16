package com.qlarr.backend.controllers

import com.qlarr.backend.api.survey.CloneRequest
import com.qlarr.backend.api.survey.SurveyDTO
import com.qlarr.backend.services.GuestSurveyService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class GuestSurveyController(val guestSurveyService: GuestSurveyService) {


    @PostMapping("/survey/{surveyId}/clone_guest")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun cloneGuestSurvey(
        @PathVariable surveyId: UUID,
        @RequestBody cloneRequest: CloneRequest
    ): ResponseEntity<SurveyDTO> {
        guestSurveyService.cloneGuestSurvey(surveyId, cloneRequest)

        return ResponseEntity.ok().build()
    }

}