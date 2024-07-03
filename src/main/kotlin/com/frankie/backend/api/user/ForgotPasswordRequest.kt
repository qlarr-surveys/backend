package com.frankie.backend.api.user

import java.util.*


data class ForgotPasswordRequest(
    val email: String,
) {
    fun email() = email.trim().lowercase()
}


data class TokenRequest(
    val token: UUID,
)
