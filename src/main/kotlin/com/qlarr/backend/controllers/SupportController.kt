package com.qlarr.backend.controllers

import com.qlarr.backend.api.support.ContactRequest
import com.qlarr.backend.services.SupportService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SupportController(
    private val supportService: SupportService
) {

    @PostMapping("/support/contact")
    fun contact(@RequestBody contactRequest: ContactRequest): ResponseEntity<Any> {
        supportService.submitContactForm(contactRequest)
        return ResponseEntity(HttpStatus.OK)
    }
}
