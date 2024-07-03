package com.frankie.backend.api.user

import java.util.UUID


data class ResetPasswordRequest(
    val refreshToken: String,
    val newPassword: String,
)
data class ConfirmUserRequest(
    val token: UUID,
    val newPassword: String,
)

data class ConfirmEmailRequest(
        val pin: String
)
