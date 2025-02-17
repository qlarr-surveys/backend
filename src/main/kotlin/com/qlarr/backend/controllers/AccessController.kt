package com.qlarr.backend.controllers

import com.qlarr.backend.api.user.*
import com.qlarr.backend.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AccessController(
        private val userService: UserService,
) {

    @PostMapping("/user/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<LoggedInUserResponse> {
        return ResponseEntity(userService.login(loginRequest), HttpStatus.OK)
    }

    @PostMapping("/user/refresh_token")
    fun refreshToken(@RequestBody refreshRequest: RefreshRequest): ResponseEntity<LoggedInUserResponse> {
        val response = userService.refreshToken(refreshRequest)
        return ResponseEntity.ok().body(response)
    }


    @PostMapping("/user/forgot_password")
    fun forgotPassword(@RequestBody forgotPasswordRequest: ForgotPasswordRequest): ResponseEntity<Any> {
        userService.forgotPassword(forgotPasswordRequest.email())
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @PostMapping("/user/reset_password")
    fun resetPassword(@RequestBody resetPasswordRequest: ResetPasswordRequest): ResponseEntity<LoggedInUserResponse> {
        val loggedInUserResponse = userService.resetPassword(resetPasswordRequest)
        return ResponseEntity(loggedInUserResponse, HttpStatus.OK)
    }
}
