package com.qlarr.backend.controllers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.api.design.DesignDto
import com.qlarr.backend.api.offline.DesignDiffDto
import com.qlarr.backend.api.offline.PublishInfo
import com.qlarr.backend.api.version.VersionDto
import com.qlarr.backend.services.DesignService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class DesignController(
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

    @PostMapping("/survey/{surveyId}/design")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    fun setDesign(
            @PathVariable surveyId: UUID,
            @RequestBody design: ObjectNode,
            @RequestParam version: Int,
            @RequestParam("sub_version") subVersion: Int
    ): ResponseEntity<DesignDto> {

        val dto = designService.setDesign(surveyId, design, version, subVersion)
        return ResponseEntity(dto, HttpStatus.OK)
    }

    @PostMapping("/survey/{surveyId}/change_code")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    fun changeCode(
            @PathVariable surveyId: UUID,
            @RequestParam from: String,
            @RequestParam to: String
    ): ResponseEntity<DesignDto> {

        val dto = designService.changeCode(surveyId, from, to)
        return ResponseEntity(dto, HttpStatus.OK)
    }

    @GetMapping("/survey/{surveyId}/design")
    @PreAuthorize("hasAnyAuthority({'super_admin','survey_admin'})")
    fun getDesign(
            @PathVariable surveyId: UUID
    ): ResponseEntity<DesignDto> {
        return ResponseEntity(designService.getDesign(surveyId), HttpStatus.OK)
    }

    @PostMapping("/survey/{surveyId}/design/publish")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun publish(
            @PathVariable surveyId: UUID,
            @RequestParam version: Int,
            @RequestParam("sub_version") subVersion: Int
    ): ResponseEntity<VersionDto> {
        return ResponseEntity(designService.publish(surveyId, version, subVersion), HttpStatus.OK)
    }
}
