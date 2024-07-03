package com.frankie.backend.persistence.entities

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "refresh_tokens")
data class RefreshTokenEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "session_id", nullable = false)
    val sessionId:UUID,

    val expiration: LocalDateTime,
)
