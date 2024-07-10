package com.frankie.backend.services

import com.frankie.backend.api.user.*
import com.frankie.backend.common.*
import com.frankie.backend.exceptions.*
import com.frankie.backend.mappers.UserMapper
import com.frankie.backend.persistence.entities.EmailChangesEntity
import com.frankie.backend.persistence.entities.RefreshTokenEntity
import com.frankie.backend.persistence.entities.UserEntity
import com.frankie.backend.persistence.repositories.EmailChangesRepository
import com.frankie.backend.persistence.repositories.RefreshTokenRepository
import com.frankie.backend.persistence.repositories.UserRepository
import com.frankie.backend.security.JwtService
import com.frankie.backend.security.JwtService.JwtResetPasswordData
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


@Service
class UserService(
        private val emailChangesRepository: EmailChangesRepository,
        private val userMapper: UserMapper,
        private val userRepository: UserRepository,
        private val jwtService: JwtService,
        private val refreshTokenRepository: RefreshTokenRepository,
        private val encoder: PasswordEncoder,
        private val emailService: EmailService,
        private val userUtils: UserUtils,
        @Value("\${frontend.host}")
        val frontendDomain: String,
) {

    fun getAllUsers(): List<UserDTO> {
        return userRepository.findAllByDeletedIsFalse()
                .map(userMapper::mapToDto)
    }

    fun getUserById(userId: UUID): UserDTO {
        return userRepository.findByIdOrNull(userId)?.let {
            userMapper.mapToDto(it)
        } ?: throw UserNotFoundException()
    }

    @Transactional
    // only user tenant, this is safe
    fun update(userId: UUID, editUserRequest: EditUserRequest): UserDTO {
        if (userId == userUtils.currentUserId()) {
            throw EditOwnUserException()
        }
        val user = userRepository.findByIdAndDeletedIsFalse(userId) ?: throw UserNotFoundException()
        if (editUserRequest.roles?.isEmpty() == true) {
            throw EmptyRolesException()
        }

        if (editUserRequest.firstName?.isValidName() == false) {
            throw InvalidFirstName()
        }
        if (editUserRequest.lastName?.isValidName() == false) {
            throw InvalidLastName()
        }
        // We don't want to change the email into a duplicate one
        val newUser = user.copy(
                firstName = editUserRequest.firstName?.trim() ?: user.firstName,
                lastName = editUserRequest.lastName?.trim() ?: user.lastName,
                // Only a super admin can edit user roles
                roles = if (!editUserRequest.roles.isNullOrEmpty()) editUserRequest.roles else user.roles,
        )
        userRepository.save(newUser)
        return userMapper.mapToDto(newUser)
    }

    fun createFirstUser() {
        if (userRepository.count() == 0L) {
            userRepository.save(UserEntity(
                    id = UUID.randomUUID(),
                    firstName = "admin",
                    lastName = "admin",
                    isConfirmed = true,
                    roles = setOf(Roles.SUPER_ADMIN),
                    password = encoder.encode("admin"),
                    email = "admin@admin.admin"
            ))
        }
    }

    @Transactional
    // It is fine to mix master and tenant because user is logged in
    fun editProfile(editProfileRequest: EditProfileRequest): UserDTO {
        val userId = userUtils.currentUserId()
        val user = userRepository.findByIdAndDeletedIsFalse(userId) ?: throw UserNotFoundException()
        val emailChanged = editProfileRequest.email() != null && user.email != editProfileRequest.email()
        val passwordChanged = editProfileRequest.newPassword != null
        if ((passwordChanged || emailChanged) && !encoder.matches(editProfileRequest.password, user.password)) {
            throw WrongCredentialsException()
        }
        if (editProfileRequest.email?.isValidEmail() == false) {
            throw InvalidEmail()
        }
        if (editProfileRequest.firstName?.isValidName() == false) {
            throw InvalidFirstName()
        }
        if (editProfileRequest.lastName?.isValidName() == false) {
            throw InvalidLastName()
        }
        // We don't want to change the email into a duplicate one
        if (emailChanged) {
            abortIfDuplicateEmail(editProfileRequest.email()!!)
        }
        val newUser = user.copy(
                firstName = editProfileRequest.firstName?.trim() ?: user.firstName,
                lastName = editProfileRequest.lastName?.trim() ?: user.lastName,
                password = editProfileRequest.newPassword?.let {
                    encoder.encode(
                            it
                    )
                } ?: user.password,
        )

        if (emailChanged) {
            sendEmailChangePin(user.email, editProfileRequest.email!!)
        }
        if (passwordChanged) {
            invalidateRefreshToken()
        }
        userRepository.save(newUser)
        return userMapper.mapToDto(newUser)
    }

    private fun sendEmailChangePin(oldEmail: String, newEmail: String) {
        val emailChangesEntity = EmailChangesEntity(
                userUtils.currentUserId(), newEmail)
        emailChangesRepository.save(emailChangesEntity)
        emailService.sendEmail(oldEmail, subject = "Email change request", body = "your pin is: ${emailChangesEntity.pin}")

    }

    @Transactional
    // It is fine to mix master and tenant because user is logged in
    fun delete(userId: UUID) {
        val user = userRepository.findByIdAndDeletedIsFalse(userId) ?: throw UserNotFoundException()
        val prefix = "deleted_"
        val newUser = user.copy(
                firstName = prefix + UUID.randomUUID().toString(),
                lastName = prefix + UUID.randomUUID().toString(),
                email = prefix + UUID.randomUUID().toString(),
                deleted = true
        )
        userRepository.save(newUser)
    }


    private fun abortIfDuplicateEmail(email: String) {
        if (emailExists(email)) {
            throw DuplicateEmailException()
        }
    }

    private fun emailExists(email: String): Boolean {
        return userRepository.findByEmailAndDeletedIsFalse(email) != null
    }


    @Transactional
    // It is fine to mix master and tenant because user is logged in
    fun create(createRequest: CreateRequest): UserDTO {
        val email = createRequest.email()
        abortIfDuplicateEmail(email)
        if (!createRequest.email.isValidEmail()) {
            throw InvalidEmail()
        }
        if (!createRequest.firstName.isValidName()) {
            throw InvalidFirstName()
        }
        if (!createRequest.lastName.isValidName()) {
            throw InvalidLastName()
        }
        val userEntity = userMapper.mapToEntity(createRequest)
        if (userEntity.roles.isEmpty()) {
            throw EmptyRolesException()
        }
        val savedEntity = try {
            userRepository.save(userEntity)
        } catch (exception: DataIntegrityViolationException) {
            // we assume here that at least only the email constraint could be violated
            throw DuplicateEmailException()
        }
        sendNewUserConfirmation(savedEntity)
        return userMapper.mapToDto(userEntity)
    }


    fun login(loginRequest: LoginRequest): LoggedInUserResponse {
        // check password
        userRepository.findByEmailAndDeletedIsFalse(loginRequest.email())?.let {
            val matchesPassword = encoder.matches(loginRequest.password, it.password)
            if (matchesPassword) {
                return loggedUserResponse(it, true)
            } else {
                throw WrongCredentialsException()
            }
        } ?: throw WrongCredentialsException()
    }

    private fun extractResetPasswordDetails(refreshToken: String): JwtResetPasswordData {
        val details = try {
            jwtService.getResetPasswordDetails(refreshToken)
        } catch (e: ExpiredJwtException) {
            throw ExpiredResetTokenException()
        } catch (e: Exception) {
            throw WrongResetTokenException()
        }
        if (!details.resetPassword) {
            throw WrongResetTokenException()
        }
        return details
    }

    fun resetPassword(resetPasswordRequest: ResetPasswordRequest): LoggedInUserResponse {
        // validate reset token
        val details = extractResetPasswordDetails(resetPasswordRequest.refreshToken)
        val userEntity = userRepository.findByEmailAndDeletedIsFalse(email = details.email)
                ?: throw WrongResetTokenException()
        val newUserEntity = userEntity.copy(
                password = encoder.encode(
                        resetPasswordRequest.newPassword
                ), isConfirmed = true
        )
        return loggedUserResponse(newUserEntity, true)
    }


    fun sendPasswordResetEmail(userEntity: UserEntity) {
        val resetToken = jwtService.generatePasswordResetToken(userEntity, newUser = false)
        emailService.sendEmail(
                to = userEntity.email,
                body = " To reset your password... Follow this link: http://$frontendDomain/reset-password?token=$resetToken",
                subject = "Your Password Reset Token"
        )
    }

    fun sendNewUserConfirmation(userEntity: UserEntity) {
        val resetToken = jwtService.generatePasswordResetToken(userEntity, newUser = true)
        emailService.sendEmail(
                to = userEntity.email,
                body = " You have been invited... Follow this link: http://$frontendDomain/reset-password?token=$resetToken",
                subject = "Invitation to join Qlarr.com"
        )
    }


    fun countUserRoles(): CountByRoleResponse {
        return userRepository.selectRoleCounts()
    }


    fun refreshToken(refreshRequest: RefreshRequest): LoggedInUserResponse {
        val token = refreshRequest.accessToken
        val details = jwtService.getJwtUserDetailsFromExpired(token)
        val userRefresh = refreshTokenRepository.getUserByRefreshToken(UUID.fromString(refreshRequest.refreshToken))
                ?: throw JwtException("Invalid or token expired try to reconnect")
        val user = userRefresh.user
        if (user.id.toString() == details.userId
                && userRefresh.refreshToken.expiration > LocalDateTime.now(ZoneOffset.UTC)
        ) {
            return loggedUserResponse(user, false, userRefresh.refreshToken)
        }
        throw JwtException("Invalid or token expired try to reconnect")
    }

    fun loggedUserResponse(
            userEntity: UserEntity,
            newRefresh: Boolean,
            refreshToken: RefreshTokenEntity? = null
    ): LoggedInUserResponse {
        val newToken = jwtService.generateAccessToken(userEntity)
        val tokenEntity = if (newRefresh) {
            refreshTokenRepository.save(
                    RefreshTokenEntity(
                            userId = userEntity.id!!,
                            sessionId = newToken.sessionId,
                            expiration = newToken.refreshTokenExpiry
                    )
            )
        } else
            refreshToken!!
        val lastLogin = if (newRefresh) LocalDateTime.now(ZoneOffset.UTC) else userEntity.lastLogin
        if (newRefresh) {
            val newUser =
                    userEntity.copy(
                            lastLogin = lastLogin
                    )
            userRepository.save(newUser)
        }
        return userMapper.mapToUserResponse(
                userEntity,
                newToken.token,
                refreshToken = tokenEntity.id!!
        )
    }

    fun invalidateRefreshToken(userToken: String? = null) {
        val token = userToken ?: userUtils.currentAuthToken()
        val sessionId = jwtService.getJwtUserDetailsBypassExpiry(token).sessionId
        sessionId?.let {
            refreshTokenRepository.deleteBySessionId(sessionId)
        } ?: refreshTokenRepository.deleteByUserId(userUtils.currentUserId())
    }

    fun forgotPassword(email: String) {
        userRepository.findByEmailAndDeletedIsFalse(email)?.let { userEntity ->
            sendPasswordResetEmail(userEntity)
        }
    }

    @Transactional
    fun confirmNewEmail(confirmEmailRequest: ConfirmEmailRequest): LoggedInUserResponse {
        val userEntity = userRepository.findByIdOrNull(userUtils.currentUserId())!!
        val emailChangesEntity = emailChangesRepository.findByIdOrNull(userUtils.currentUserId())
                ?: throw WrongEmailChangePinException()
        if (emailChangesEntity.pin != confirmEmailRequest.pin) {
            throw WrongEmailChangePinException()
        }
        val newEntity = userEntity.copy(email = emailChangesEntity.newEmail)
        userRepository.save(newEntity)
        emailChangesRepository.deleteById(userUtils.currentUserId())
        invalidateRefreshToken()
        return loggedUserResponse(newEntity, true)
    }


}
