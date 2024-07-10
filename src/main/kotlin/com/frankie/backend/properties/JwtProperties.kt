package com.frankie.backend.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class JwtProperties(
    @Value("\${jwt.secret}") val secret: String,
    @Value("\${jwt.activeExpirationMs}") val activeExpiration: Long,
    @Value("\${jwt.resetExpirationMs}") val resetExpiration: Long,
    @Value("\${jwt.resetExpirationForNewUsersMs}") val resetExpirationForNewUsersMs: Long,
    @Value("\${jwt.refreshExpirationMs}") val refreshExpiration: Long,
)
