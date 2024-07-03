package com.frankie.backend.api.user


data class LoginRequest(
    //@Email
    val email: String,
    //@NotEmpty
    val password: String,
) {
    fun email() = email.trim().lowercase()
}


data class GoogleSignIn(
    val credential: String,
    val clientId: String
)