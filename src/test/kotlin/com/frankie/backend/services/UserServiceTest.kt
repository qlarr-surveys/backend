package com.frankie.backend.services

import com.frankie.backend.api.user.*
import com.frankie.backend.common.RECENT_LOGIN_SPAN
import com.frankie.backend.common.UserUtils
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.EditOwnUserException
import com.frankie.backend.exceptions.EmptyRolesException
import com.frankie.backend.exceptions.WrongResetTokenException
import com.frankie.backend.mappers.UserMapper
import com.frankie.backend.persistence.entities.RefreshTokenEntity
import com.frankie.backend.persistence.entities.UserEntity
import com.frankie.backend.persistence.repositories.EmailChangesRepository
import com.frankie.backend.persistence.repositories.RefreshTokenRepository
import com.frankie.backend.persistence.repositories.UserRepository
import com.frankie.backend.security.JwtService
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.Assert.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class UserServiceTest {


    @MockK
    private lateinit var jwtService: JwtService

    @MockK
    private lateinit var userUtils: UserUtils

    @Suppress("unused")
    val frontendDomain: String = ""

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var emailService: EmailService

    @MockK
    private lateinit var encoder: PasswordEncoder

    @MockK
    private lateinit var userMapper: UserMapper

    @MockK
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockK
    private lateinit var emailChangesRepository: EmailChangesRepository

    @InjectMockKs
    private lateinit var userService: UserService


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }


    @Test
    fun `user can edit their firstName`() {
        val newName = "koko"
        val userId = UUID.randomUUID()
        val user = generateSurveyor(userId)
        val expectedUser = user.copy(firstName = newName)
        every { encoder.encode(any()) } returns ""
        every { userUtils.currentUserId() } returns userId
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        every { userRepository.save(expectedUser) } returns expectedUser

        every { userMapper.mapToDto(expectedUser) } returns UserDTO(
                id = expectedUser.id!!,
                firstName = expectedUser.firstName,
                lastName = expectedUser.lastName,
                email = expectedUser.email,
                roles = setOf(Roles.SUPER_ADMIN)
        )
        val newUser = userService.editProfile(EditProfileRequest(firstName = newName))
        verify(exactly = 1) { userRepository.save(expectedUser) }
        Assertions.assertEquals(newName, newUser.firstName)
    }

    @Test
    fun `super admin edit any user's firstName`() {
        val userId = UUID.randomUUID()
        val newName = "koko"
        val user = generateSurveyor(userId)
        val expectedUser = user.copy(firstName = newName)
        every { encoder.encode(any()) } returns ""
        every { userUtils.currentUserId() } returns UUID.randomUUID()
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        every { userRepository.save(expectedUser) } returns expectedUser
        every { userMapper.mapToDto(expectedUser) } returns UserDTO(
                id = expectedUser.id!!,
                firstName = expectedUser.firstName,
                lastName = expectedUser.lastName,
                email = expectedUser.email,
                roles = setOf(Roles.SUPER_ADMIN)
        )
        val newUser = userService.update(userId, EditUserRequest(firstName = newName))
        verify { userRepository.save(expectedUser) }
        verify { userRepository.save(expectedUser) }




        Assertions.assertEquals(newName, newUser.firstName)
    }

    @Test
    fun `super admin edit any user's roles`() {
        val newRoles = setOf(Roles.SURVEYOR)
        val userId = UUID.randomUUID()
        val user = generateSurveyor(userId)
        val expectedUser = user.copy(roles = newRoles)
        every { encoder.encode(any()) } returns ""
        every { userUtils.currentUserId() } returns UUID.randomUUID()
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        every { userRepository.save(expectedUser) } returns expectedUser

        every { userMapper.mapToDto(expectedUser) } returns UserDTO(
                id = expectedUser.id!!,
                firstName = expectedUser.firstName,
                lastName = expectedUser.lastName,
                email = expectedUser.email,
                roles = expectedUser.roles
        )
        val newUser = userService.update(userId, EditUserRequest(roles = newRoles))
        verify(exactly = 1) { userRepository.save(expectedUser) }
        Assertions.assertEquals(expectedUser.roles, newUser.roles)
    }


    @Test
    fun `cannot edit own user`() {
        val userId = UUID.randomUUID()
        val user = generateSurveyor(userId)
        every { userUtils.currentUserId() } returns userId
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        assertThrows(EditOwnUserException::class.java) {
            userService.update(userId, EditUserRequest(roles = setOf(Roles.SURVEY_ADMIN)))
        }
    }

    @Test
    fun `cannot set empty roles`() {
        val userId = UUID.randomUUID()
        val user = generateSurveyor(userId)
        every { userUtils.currentUserId() } returns UUID.randomUUID()
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        assertThrows(EmptyRolesException::class.java) {
            userService.update(userId, EditUserRequest(roles = emptySet()))
        }
    }


    @Test
    fun reset() {
        val userId = UUID.randomUUID()
        val savedUser = slot<UserEntity>()
        val refreshTokenId = UUID.randomUUID()
        every { refreshTokenRepository.save(any()) } returns RefreshTokenEntity(
                refreshTokenId,
                userId,
                sessionId = UUID.randomUUID(),
                expiration = nowUtc().plusSeconds(1000)
        )
        every { encoder.encode(any()) } returns ""
        every { jwtService.getResetPasswordDetails(any()) } returns JwtService.JwtResetPasswordData(
                "",
                true
        )
        every { jwtService.generateAccessToken(any()) } returns AccessToken(
                UUID.randomUUID(), "", UUID.randomUUID(),
                LocalDateTime.now()
        )
        every { jwtService.generatePasswordResetToken(any()) } returns "resert_token"
        val user =
                generateSurveyor(userId, nowUtc().minusSeconds(RECENT_LOGIN_SPAN / 1000L + 1))
        every { userRepository.findByEmailAndDeletedIsFalse(any()) } returns user
        every { userRepository.save(capture(savedUser)) } returns user.copy(password = "nePAss")
        every { userMapper.mapToUserResponse(any(), any(), any()) } returns LoggedInUserResponse(
                id = UUID.randomUUID(),
                firstName = "",
                lastName = "",
                accessToken = "",
                refreshToken = UUID.randomUUID(),
                email = "",
                roles = setOf()
        )
        val authToken = jwtService.generatePasswordResetToken(user)
        userService.resetPassword(ResetPasswordRequest(authToken, "nePAss"))
        Assertions.assertNotEquals(user.password, savedUser.captured.password)
    }

    @Test
    fun `reset fails with access token as refresh`() {
        val userId = UUID.randomUUID()
        val user =
                generateSurveyor(userId, nowUtc().minusSeconds(RECENT_LOGIN_SPAN / 1000L + 1))
        every { jwtService.generateAccessToken(any()) } returns AccessToken(
                UUID.randomUUID(), "", UUID.randomUUID(),
                LocalDateTime.now()
        )
        val authToken = jwtService.generateAccessToken(user).token
        assertThrows(WrongResetTokenException::class.java) {
            userService.resetPassword(ResetPasswordRequest(authToken, "nePAss"))
        }
    }

    @Test
    fun `reset fails with wrong token`() {
        val authToken = "asdfasdfasdfasdfasdf"
        assertThrows(WrongResetTokenException::class.java) {
            userService.resetPassword(ResetPasswordRequest(authToken, "nePAss"))
        }
    }


    companion object {
        fun generateSurveyor(userId: UUID, lastLogin: LocalDateTime = LocalDateTime.now()) = UserEntity(
                id = userId,
                firstName = "firstName",
                lastName = "lastName",
                email = "emailj@email.com",
                password = "password",
                roles = setOf(Roles.SURVEYOR),
                lastLogin = lastLogin,
        )
    }


}
