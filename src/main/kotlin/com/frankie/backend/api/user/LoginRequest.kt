package com.frankie.backend.api.user


data class LoginRequest(
    //@Email
    val email: String,
    //@NotEmpty
    val password: String,
) {
    fun email() = email.trim().lowercase()
}