package com.qlarr.backend.controllers

import com.qlarr.backend.api.event.EventDTO
import com.qlarr.backend.common.DATE_TIME_UTC_FORMAT
import com.qlarr.backend.services.EventService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
class EventController(
    private val eventService: EventService,
) {

    @PreAuthorize("hasAnyAuthority({'super_admin'})")
    @GetMapping("/user/{userId}/event")
    fun getAll(
        @PathVariable userId: UUID,
        @DateTimeFormat(pattern = DATE_TIME_UTC_FORMAT)
        @RequestParam from: LocalDateTime? = null,
        @DateTimeFormat(pattern = DATE_TIME_UTC_FORMAT)
        @RequestParam to: LocalDateTime? = null,
    ): ResponseEntity<List<EventDTO>> {
        return ResponseEntity(eventService.getAllEventsForUser(userId, from, to), HttpStatus.OK)
    }

    @PostAuthorize("authentication.authenticated")
    @PostMapping("/user/{userId}/event")
    fun create(
        @PathVariable userId: UUID,
        @RequestBody eventDTO: EventDTO,
    ): ResponseEntity<Any> {
        eventService.createNewUserEvent(userId, eventDTO)
        return ResponseEntity(HttpStatus.CREATED)
    }
}
