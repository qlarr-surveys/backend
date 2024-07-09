package com.frankie.backend.persistence.entities

import com.frankie.backend.api.user.Roles
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
data class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "first_name", nullable = false)
    val firstName: String,

    @Column(name = "last_name", nullable = false)
    val lastName: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    val deleted: Boolean = false,

    @Enumerated(EnumType.STRING)
    val roles: Set<Roles>,

    @Column(name = "last_login", nullable = true)
    val lastLogin: LocalDateTime? = null
)
