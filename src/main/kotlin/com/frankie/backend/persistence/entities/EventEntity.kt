package com.frankie.backend.persistence.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "events")
data class EventEntity(

    @Id
    @GeneratedValue
    @UuidGenerator
    val id: UUID? = null,

    @Column(nullable = false)
    val time: LocalDateTime,

    @Column(nullable = false)
    val name: String,

    val details: String? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,
)
