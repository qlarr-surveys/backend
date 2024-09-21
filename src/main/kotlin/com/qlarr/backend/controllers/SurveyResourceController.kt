package com.qlarr.backend.controllers

import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.services.SurveyResourceService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
class SurveyResourceController(
        private val resourceService: SurveyResourceService,
) {

    @PostMapping("/survey/{surveyId}/resource")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    fun uploadResource(
        @PathVariable surveyId: UUID,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FileInfo> {
        return resourceService.uploadResource(surveyId, file)
    }

    @GetMapping("/survey/{surveyId}/resource/{fileName}")
    fun downloadResource(
        request: HttpServletRequest,
        @PathVariable surveyId: UUID,
        @PathVariable fileName: String
    ): ResponseEntity<InputStreamResource> {
        return resourceService.downloadResource(request.serverName, surveyId, fileName)
    }

    @DeleteMapping("/survey/{surveyId}/resource/{fileName}")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun deleteResource(@PathVariable surveyId: UUID, @PathVariable fileName: String): ResponseEntity<Any> {
        return resourceService.removeResource(surveyId, fileName)
    }
}
