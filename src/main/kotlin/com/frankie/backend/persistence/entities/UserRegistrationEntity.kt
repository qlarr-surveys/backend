package com.frankie.backend.persistence.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "user_registration")
data class UserRegistrationEntity(

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @UuidGenerator
        val id: UUID? = null,

        @Column(nullable = false, updatable = false)
        val email: String,
)
