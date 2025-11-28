package com.qlarr.backend.controllers

import com.qlarr.backend.api.survey.AutoCompleteFileInfo
import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.services.SurveyResourceService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
class AutoCompleteController(
        private val resourceService: SurveyResourceService,
) {

    @PostMapping("/autocomplete/{surveyId}/{componentId}")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    fun uploadAutoCompleteResource(
        @PathVariable surveyId: UUID,
        @PathVariable componentId: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<AutoCompleteFileInfo> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().build()
        }
        return resourceService.uploadAutoCompleteResource(surveyId,componentId, file)
    }

    @GetMapping("/survey/{surveyId}/autocomplete/{uuid}")
    fun searchAutoComplete(
        @PathVariable uuid: String,
        @RequestParam("q") searchTerm: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<Any>> {
        val results = resourceService.search(uuid, searchTerm, limit)
        return ResponseEntity.ok(results)
    }

}
