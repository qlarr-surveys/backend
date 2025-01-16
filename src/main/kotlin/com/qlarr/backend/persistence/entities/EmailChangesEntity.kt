package com.qlarr.backend.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*
import kotlin.random.Random

@Entity
@Table(name = "email_changes")
data class EmailChangesEntity(

        @Id
        @Column(name = "user_id")
        val id: UUID,

        @Column(name = "new_email", nullable = false)
        val newEmail: String,

        @Column(name = "pin", nullable = false)
        val pin: String = Random.nextInt(100000, 999999).toString()
)
