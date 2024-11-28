package com.qlarr.backend.controllers

import com.qlarr.backend.api.response.ResponseCountDto
import com.qlarr.backend.api.response.ResponseUploadFile
import com.qlarr.backend.api.response.UploadResponseRequestData
import com.qlarr.backend.services.ResponseOpsService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
class ResponseOpsController(
        private val responseOpsService: ResponseOpsService
) {

    @PostMapping("/survey/{surveyId}/response/attach/{responseId}/{questionId}")
    fun uploadResponseFile(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID,
            @PathVariable questionId: String,
            @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ResponseUploadFile> {
        val result = responseOpsService.uploadResponseFile(request.serverName, surveyId, responseId, questionId, false, file)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/survey/{surveyId}/response/attach/{responseId}/{questionId}")
    fun getResponseFileNew(
        request: HttpServletRequest,
        @PathVariable surveyId: UUID,
        @PathVariable responseId: UUID,
        @PathVariable questionId: String
    ): ResponseEntity<InputStreamResource> {
        return responseOpsService.downloadFileNew(surveyId, responseId, questionId)
    }

    @PostMapping("/survey/{surveyId}/response/preview/attach/{responseId}/{questionId}")
    fun uploadResponsePreviewFile(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID,
            @PathVariable questionId: String,
            @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ResponseUploadFile> {
        val result = responseOpsService.uploadResponseFile(request.serverName, surveyId, responseId, questionId, true, file)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','surveyor'})")
    @PostMapping("/survey/{surveyId}/offline/response/upload/{fileName}")
    fun uploadOfflineResponseFile(
            request: HttpServletRequest,
            @PathVariable fileName: String,
            @PathVariable surveyId: UUID,
            @RequestPart file: MultipartFile
    ): ResponseEntity<ResponseUploadFile> {
        val result = responseOpsService.uploadOfflineResponseFile(surveyId, fileName, file)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','surveyor'})")
    @PostMapping("/survey/{surveyId}/offline/response/upload/{filename}/exists")
    fun isOfflineFileUploaded(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable filename: String
    ): ResponseEntity<Boolean> {
        val result = responseOpsService.isOfflineFileAlreadyUploaded(surveyId, filename)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin','surveyor'})")
    @PostMapping("/survey/{surveyId}/response/{responseId}/upload")
    fun uploadOfflineSurveyResponse(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID,
            @RequestBody uploadSurveyRequestData: UploadResponseRequestData
    ): ResponseEntity<ResponseCountDto> {
        val count = responseOpsService.uploadOfflineSurveyResponse(surveyId, responseId, uploadSurveyRequestData)
        return ResponseEntity(count, HttpStatus.OK)
    }

    @GetMapping("/survey/{surveyId}/response/attach/{filename}")
    fun getResponseFile(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable filename: UUID
    ): ResponseEntity<InputStreamResource> {
        return responseOpsService.downloadFile(request.serverName, surveyId, filename)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    @DeleteMapping("/survey/{surveyId}/response/{responseId}")
    fun deleteResponse(
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID
    ): ResponseEntity<Any> {
        responseOpsService.deleteResponse(surveyId, responseId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
