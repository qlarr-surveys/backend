package com.qlarr.backend.api.user

import java.util.*


data class LoggedInUserResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val accessToken: String,
    val refreshToken: UUID,
    val email: String,
    val roles: Set<Roles>
)