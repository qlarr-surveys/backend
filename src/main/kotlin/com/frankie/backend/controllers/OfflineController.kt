package com.frankie.backend.controllers

import com.frankie.backend.api.offline.DesignDiffDto
import com.frankie.backend.api.offline.PublishInfo
import com.frankie.backend.api.survey.OfflineSurveyDto
import com.frankie.backend.common.UserUtils
import com.frankie.backend.services.DesignService
import com.frankie.backend.services.SurveyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class OfflineController(
        private val surveyService: SurveyService,
        private val designService: DesignService
) {

    @PostMapping("/survey/{surveyId}/offline/design")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','surveyor'})")
    fun offlineDesignDiff(
            @PathVariable surveyId: UUID,
            @RequestBody publishInfo: PublishInfo
    ): ResponseEntity<DesignDiffDto> {
        return ResponseEntity(designService.offlineDesignDiff(surveyId, publishInfo), HttpStatus.OK)
    }

    @PostMapping("/guest/survey/{surveyId}/offline/design")
    fun offlineDesignDiffForGuest(
            @PathVariable surveyId: UUID,
            @RequestBody publishInfo: PublishInfo
    ): ResponseEntity<DesignDiffDto> {
        return ResponseEntity(designService.offlineDesignDiff(surveyId, publishInfo), HttpStatus.OK)
    }

    @GetMapping("/survey/offline")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','surveyor'})")
    fun surveysForOffline(
    ): ResponseEntity<List<OfflineSurveyDto>> {
        return ResponseEntity(surveyService.surveysForOffline(), HttpStatus.OK)
    }

}
