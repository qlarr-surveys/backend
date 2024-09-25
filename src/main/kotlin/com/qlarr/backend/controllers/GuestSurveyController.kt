package com.qlarr.backend.controllers

import com.qlarr.backend.services.GuestSurveyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class GuestSurveyController(val guestSurveyService: GuestSurveyService) {

    @GetMapping("/survey/{surveyId}/clone_guest")
    fun cloneGuestSurvey(@PathVariable surveyId: UUID): ResponseEntity<Void> {
        if (!guestSurveyService.cloneGuestSurvey(surveyId)) {
            return ResponseEntity.internalServerError().build()
        }

        return ResponseEntity.ok().build()
    }

}