package com.frankie.backend.controllers

import com.amazonaws.services.s3.Headers
import com.frankie.backend.api.response.ResponseUploadFile
import com.frankie.backend.api.response.ResponsesDto
import com.frankie.backend.api.response.UploadResponseRequestData
import com.frankie.backend.api.survey.OfflineSurveyDto
import com.frankie.backend.api.user.*
import com.frankie.backend.exceptions.UnrecognizedZoneException
import com.frankie.backend.services.ResponseService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.ZoneId
import java.util.*

@RestController
class ResponseController(
        private val responseService: ResponseService
) {

    @PostMapping("/survey/{surveyId}/response/attach/{responseId}/{questionId}")
    fun uploadResponseFile(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID,
            @PathVariable questionId: String,
            @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ResponseUploadFile> {
        val result = responseService.uploadResponseFile(request.serverName, surveyId, responseId, questionId, false, file)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping("/survey/{surveyId}/response/preview/attach/{responseId}/{questionId}")
    fun uploadResponsePreviewFile(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID,
            @PathVariable questionId: String,
            @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ResponseUploadFile> {
        val result = responseService.uploadResponseFile(request.serverName, surveyId, responseId, questionId, true, file)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin, surveyor'})")
    @PostMapping("/survey/{surveyId}/offline/response/upload/{fileName}")
    fun uploadOfflineResponseFile(
            request: HttpServletRequest,
            @PathVariable fileName: String,
            @PathVariable surveyId: UUID,
            @RequestPart file: MultipartFile
    ): ResponseEntity<ResponseUploadFile> {
        val result = responseService.uploadOfflineResponseFile(surveyId, fileName, file)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin, surveyor'})")
    @PostMapping("/survey/{surveyId}/offline/response/upload/{filename}/exists")
    fun isOfflineFileUploaded(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable filename: String
    ): ResponseEntity<Boolean> {
        val result = responseService.isOfflineFileAlreadyUploaded(surveyId, filename)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin, surveyor'})")
    @PostMapping("/survey/{surveyId}/response/{responseId}/upload")
    fun uploadOfflineSurveyResponse(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID,
            @RequestBody uploadSurveyRequestData: UploadResponseRequestData
    ): ResponseEntity<OfflineSurveyDto> {
        val result = responseService.uploadOfflineSurveyResponse(surveyId, responseId, uploadSurveyRequestData)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/survey/{surveyId}/response/attach/{filename}")
    fun getResponseFile(
            request: HttpServletRequest,
            @PathVariable surveyId: UUID,
            @PathVariable filename: UUID
    ): ResponseEntity<InputStreamResource> {
        return responseService.downloadFile(request.serverName, surveyId, filename)
    }

    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    @DeleteMapping("/survey/{surveyId}/response/{responseId}")
    fun deleteResponse(
            @PathVariable surveyId: UUID,
            @PathVariable responseId: UUID
    ): ResponseEntity<Any> {
        responseService.deleteResponse(surveyId, responseId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

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
                .header(Headers.CONTENT_TYPE, "text/csv")
                .header("Content-Disposition", "attachment; filename=\"$surveyId-responses-export.csv\"")
                .body(result)
    }
}
