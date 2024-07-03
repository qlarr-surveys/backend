package com.frankie.backend.persistence.repositories

import com.frankie.backend.persistence.entities.UserRegistrationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRegistrationRepository : JpaRepository<UserRegistrationEntity, UUID> {

    // This is only for testing purposes, because intercepting emails if really quite a hassle

    fun findByEmail(email: String): List<UserRegistrationEntity>


}
