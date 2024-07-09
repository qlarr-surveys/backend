package com.frankie.backend.mappers

import com.frankie.backend.api.user.CreateRequest
import com.frankie.backend.api.user.LoggedInUserResponse
import com.frankie.backend.api.user.UserDTO
import com.frankie.backend.persistence.entities.UserEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserMapper(private val encoder: PasswordEncoder) {

    fun mapToUserResponse(
            userEntity: UserEntity,
            accessToken: String,
            refreshToken: UUID,
    ): LoggedInUserResponse {
        return LoggedInUserResponse(
                id = userEntity.id!!,
                firstName = userEntity.firstName,
                lastName = userEntity.lastName,
                accessToken = accessToken,
                refreshToken = refreshToken,
                email = userEntity.email,
                roles = userEntity.roles
        )
    }

    fun mapToDto(userEntity: UserEntity): UserDTO {
        return UserDTO(
                id = userEntity.id!!,
                firstName = userEntity.firstName,
                lastName = userEntity.lastName,
                email = userEntity.email,
                roles = userEntity.roles,
        )
    }

    fun mapToEntity(createRequest: CreateRequest): UserEntity {
        val randomPassword = randomPassword(createRequest.firstName, createRequest.lastName)
        val password = encoder.encode(randomPassword)
        return UserEntity(
                firstName = createRequest.firstName.trim(),
                lastName = createRequest.lastName.trim(),
                email = createRequest.email(),
                password = password,
                roles = createRequest.roles,
        )
    }

    private fun randomPassword(firstName: String, lastName: String): String {
        return String.format("%sUpdateMe%s", firstName, lastName)
    }
}
