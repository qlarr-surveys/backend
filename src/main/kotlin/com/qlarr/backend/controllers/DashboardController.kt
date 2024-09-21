package com.qlarr.backend.controllers

import com.qlarr.backend.api.survey.OfflineSurveyDto
import com.qlarr.backend.api.survey.SurveysDto
import com.qlarr.backend.services.SurveyDashboardService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class DashboardController(
        private val surveyDashboardService: SurveyDashboardService,
) {


    @GetMapping("/survey/all")
    fun getAll(
            @RequestParam page: Int?,
            @RequestParam("per_page") perPage: Int?,
            @RequestParam("sort_by") sortBy: String?,
            @RequestParam status: String?
    ): ResponseEntity<SurveysDto> {
        val surveyDTOList = surveyDashboardService.getAllSurveys(
                page, perPage, sortBy, status
        )
        return ResponseEntity(surveyDTOList, HttpStatus.OK)
    }

    @GetMapping("/survey/offline")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','surveyor'})")
    fun surveysForOffline(
    ): ResponseEntity<List<OfflineSurveyDto>> {
        return ResponseEntity(surveyDashboardService.surveysForOffline(), HttpStatus.OK)
    }

}
