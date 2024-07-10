package com.frankie.backend.api.user


data class ResetPasswordRequest(
    val refreshToken: String,
    val newPassword: String,
)

data class ConfirmEmailRequest(
        val pin: String
)
