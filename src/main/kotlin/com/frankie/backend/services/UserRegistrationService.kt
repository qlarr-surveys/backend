package com.frankie.backend.services

import com.frankie.backend.persistence.entities.UserRegistrationEntity
import com.frankie.backend.persistence.repositories.UserRegistrationRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserRegistrationService(
        private val userRegistrationRepository: UserRegistrationRepository,
) {
    fun addUserRegistration(email: String): UUID {
        val userRegistrationEntity = UserRegistrationEntity(email = email)
        return userRegistrationRepository.save(userRegistrationEntity).id!!
    }

    fun getUserRegistration(token: UUID): UserRegistrationEntity? {
        return userRegistrationRepository.findByIdOrNull(token)
    }

    fun deleteUserRegistration(id: UUID) {
        userRegistrationRepository.deleteById(id)
    }
}
