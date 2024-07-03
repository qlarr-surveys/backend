package com.frankie.backend.api.user

import java.time.LocalDateTime
import java.util.*

data class AccessToken(
    val sessionId: UUID,
    val token: String,
    val refreshToken: UUID,
    val refreshTokenExpiry: LocalDateTime
)