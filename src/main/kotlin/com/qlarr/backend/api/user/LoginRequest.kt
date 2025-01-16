package com.qlarr.backend.api.user


data class LoginRequest(
    //@Email
    val email: String,
    //@NotEmpty
    val password: String,
) {
    fun email() = email.trim().lowercase()
}