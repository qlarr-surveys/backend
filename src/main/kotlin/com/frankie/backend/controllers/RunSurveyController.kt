package com.frankie.backend.controllers

import com.frankie.backend.api.runsurvey.NavigateRequest
import com.frankie.backend.api.runsurvey.RunSurveyDto
import com.frankie.backend.api.runsurvey.StartRequest
import com.frankie.backend.api.user.*
import com.frankie.backend.services.RunSurveyService
import com.frankie.expressionmanager.model.SurveyMode
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class RunSurveyController(
        private val navigationService: RunSurveyService,
) {

    @PostMapping("/survey/{surveyId}/run/start")
    fun start(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @RequestBody startRequest: StartRequest
    ): ResponseEntity<RunSurveyDto> {
        return ResponseEntity(
                navigationService.start(
                        surveyId,
                        startRequest,
                        false,
                        SurveyMode.ONLINE
                ),
                HttpStatus.OK
        )
    }

    @PostMapping("/survey/{surveyId}/preview/start")
    fun startPreview(
            request: HttpServletRequest,
            @RequestParam mode: String?,
            @PathVariable surveyId: UUID,
            @RequestBody startRequest: StartRequest
    ): ResponseEntity<RunSurveyDto> {
        return ResponseEntity(
                navigationService.start(
                        surveyId,
                        startRequest,
                        true,
                        mode.toSurveyMode(SurveyMode.ONLINE)
                ), HttpStatus.OK
        )
    }

    @PostMapping("/survey/{surveyId}/run/navigate")
    fun navigate(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @RequestBody navigateRequest: NavigateRequest
    ): ResponseEntity<RunSurveyDto> {
        return ResponseEntity(
                navigationService.navigate(
                        surveyId,
                        navigateRequest,
                        false,
                        SurveyMode.ONLINE
                ), HttpStatus.OK
        )
    }

    @PostMapping("/survey/{surveyId}/preview/navigate")
    fun navigatePreview(
            request: HttpServletRequest,
            @RequestParam mode: String?,
            @PathVariable surveyId: UUID,
            @RequestBody navigateRequest: NavigateRequest
    ): ResponseEntity<RunSurveyDto> {
        return ResponseEntity(
                navigationService.navigate(
                        surveyId,
                        navigateRequest,
                        true,
                        mode.toSurveyMode(SurveyMode.ONLINE)
                ),
                HttpStatus.OK
        )
    }

    @GetMapping("/survey/{surveyId}/run/runtime.js")
    fun runtimeJs(request: HttpServletRequest, @PathVariable surveyId: UUID): ResponseEntity<String> {
        return ResponseEntity(navigationService.runtimeJs(surveyId), HttpStatus.OK)
    }

    @GetMapping("/survey/{surveyId}/preview/runtime.js")
    fun runtimeJsPreview(request: HttpServletRequest, @PathVariable surveyId: UUID): ResponseEntity<String> {
        return ResponseEntity(navigationService.runtimeJs(surveyId, true), HttpStatus.OK)
    }
}

fun String?.toSurveyMode(default: SurveyMode): SurveyMode = this?.lowercase().run {
    if (this == "offline") SurveyMode.OFFLINE else if (this == "online") SurveyMode.ONLINE else default
}
