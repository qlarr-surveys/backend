package com.frankie.backend.services

import com.frankie.backend.api.user.*
import com.frankie.backend.common.RECENT_LOGIN_SPAN
import com.frankie.backend.common.UserUtils
import com.frankie.backend.common.nowUtc
import com.frankie.backend.common.tenantIdToSchema
import com.frankie.backend.exceptions.EditOwnUserException
import com.frankie.backend.exceptions.EmptyRolesException
import com.frankie.backend.exceptions.WrongResetTokenException
import com.frankie.backend.mappers.UserMapper
import com.frankie.backend.multitenancy.entities.TenantRegistrationEntity
import com.frankie.backend.multitenancy.service.GlobalUserService
import com.frankie.backend.multitenancy.service.TenantManagementService
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    private val encoder = BCryptPasswordEncoder()
    private val userMapper = UserMapper(encoder)

    @MockK
    private lateinit var jwtService: JwtService

    @MockK
    private lateinit var googleAuthService: GoogleAuthService


    @MockK
    private lateinit var userUtils: UserUtils

    @Suppress("unused")
    val frontendDomain: String = ""

    @MockK
    private lateinit var globalUserService: GlobalUserService

    @MockK
    private lateinit var tenantManagementService: TenantManagementService

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var emailService: EmailService

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
        every { userUtils.currentUserId() } returns userId
        every { userUtils.isSuperAdmin() } returns false
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        every { userRepository.save(expectedUser) } returns expectedUser

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
        every { userUtils.currentUserId() } returns UUID.randomUUID()
        every { userUtils.isSuperAdmin() } returns true
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        every { userRepository.save(expectedUser) } returns expectedUser
        val newUser = userService.update(userId, EditUserRequest(firstName = newName))
        verify { userRepository.save(expectedUser) }
        Assertions.assertEquals(newName, newUser.firstName)
    }

    @Test
    fun `super admin edit any user's roles`() {
        val newRoles = setOf(Roles.SURVEYOR)
        val userId = UUID.randomUUID()
        val user = generateSurveyor(userId)
        val expectedUser = user.copy(roles = newRoles)
        every { userUtils.currentUserId() } returns UUID.randomUUID()
        every { userUtils.isSuperAdmin() } returns true
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        every { userRepository.save(expectedUser) } returns expectedUser

        val newUser = userService.update(userId, EditUserRequest(roles = newRoles))
        verify(exactly = 1) { userRepository.save(expectedUser) }
        Assertions.assertEquals(expectedUser.roles, newUser.roles)
    }


    @Test
    fun `cannot edit own user`() {
        val userId = UUID.randomUUID()
        val user = generateSurveyor(userId)
        every { userUtils.currentUserId() } returns userId
        every { userUtils.isSuperAdmin() } returns true
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
        every { userUtils.isSuperAdmin() } returns true
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns user
        assertThrows(EmptyRolesException::class.java) {
            userService.update(userId, EditUserRequest(roles = emptySet()))
        }
    }


    @Test
    fun reset() {
        val userId = UUID.randomUUID()
        val tenantId = UUID.randomUUID()
        val savedUser = slot<UserEntity>()
        val refreshTokenId = UUID.randomUUID()
        every { refreshTokenRepository.save(any()) } returns RefreshTokenEntity(
                refreshTokenId,
                userId,
                sessionId = UUID.randomUUID(),
                expiration = nowUtc().plusSeconds(1000)
        )
        every { jwtService.getResetPasswordDetails(any()) } returns JwtService.JwtResetPasswordData(
                "",
                UUID.randomUUID(), true
        )
        every { jwtService.generateAccessToken(any(), any()) } returns AccessToken(
                UUID.randomUUID(), "", UUID.randomUUID(),
                LocalDateTime.now()
        )
        every { jwtService.generatePasswordResetToken(any(), any()) } returns "resert_token"
        val user =
                generateSurveyor(userId, nowUtc().minusSeconds(RECENT_LOGIN_SPAN / 1000L + 1))
        every { userUtils.tenantId() } returns tenantId
        every { userRepository.findByEmailAndDeletedIsFalse(any()) } returns user
        every { userRepository.save(capture(savedUser)) } returns user.copy(password = "nePAss")
        val authToken = jwtService.generatePasswordResetToken(user, tenantId)
        userService.resetPassword(ResetPasswordRequest(authToken, "nePAss"))
        Assertions.assertNotEquals(user.password, savedUser.captured.password)
    }

    @Test
    fun `reset fails with access token as refresh`() {
        val userId = UUID.randomUUID()
        val tenantId = UUID.randomUUID()
        val user =
                generateSurveyor(userId, nowUtc().minusSeconds(RECENT_LOGIN_SPAN / 1000L + 1))
        every { userUtils.tenantId() } returns tenantId
        every { jwtService.generateAccessToken(any(), any()) } returns AccessToken(
                UUID.randomUUID(), "", UUID.randomUUID(),
                LocalDateTime.now()
        )
        val authToken = jwtService.generateAccessToken(user, tenantId.toString()).token
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

    // just verifying that the current sequence is happening
    @Test
    fun `confirms admin`() {
        val userId = UUID.randomUUID()
        val tenantRegistrationEntity = TenantRegistrationEntity(
                userId, "firstName", "lastName", "email", "password", nowUtc(), false
        )

        val tenantIdForCreate = slot<UUID>()
        val tenantIdForUser = slot<UUID>()
        val savedUser = slot<UserEntity>()
        val userEntity = userMapper.mapToEntity(tenantRegistrationEntity).copy(id = UUID.randomUUID())
        every { globalUserService.getTenantRegistration(userId) } returns tenantRegistrationEntity
        every { userUtils.tenantId() } returns UUID.randomUUID()
        every { globalUserService.emailExists(tenantRegistrationEntity.email) } returns false

        justRun {
            tenantManagementService.createTenant(
                    capture(tenantIdForCreate),
                    any()
            )
        }
        justRun { globalUserService.addGlobalUser(tenantRegistrationEntity.email, capture(tenantIdForUser)) }
        justRun { globalUserService.setConfirmed(tenantRegistrationEntity) }
        justRun { globalUserService.deleteTenantRegistration(any()) }
        every { userRepository.save(capture(savedUser)) } returns userEntity
        val refreshTokenId = UUID.randomUUID()
        every { jwtService.generateAccessToken(any(), any()) } returns AccessToken(
                UUID.randomUUID(), "", UUID.randomUUID(),
                LocalDateTime.now()
        )
        every { refreshTokenRepository.save(any()) } returns RefreshTokenEntity(
                refreshTokenId,
                userId,
                sessionId = UUID.randomUUID(),
                expiration = nowUtc().plusSeconds(1000)
        )

        val loggedInUserResponse = userService.confirmAdmin(userId)

        Assertions.assertEquals(tenantIdForUser.captured, tenantIdForCreate.captured)
        verify(exactly = 1) { globalUserService.getTenantRegistration(userId) }
        verify(exactly = 1) { globalUserService.emailExists(tenantRegistrationEntity.email) }
        verify(exactly = 1) {
            tenantManagementService.createTenant(
                    tenantIdForCreate.captured,
                    tenantIdToSchema(tenantIdForCreate.captured),
            )
        }
        verify(exactly = 1) {
            globalUserService.addGlobalUser(
                    tenantRegistrationEntity.email,
                    tenantIdForUser.captured
            )
        }
        verify(exactly = 1) { globalUserService.setConfirmed(tenantRegistrationEntity) }
        verify(exactly = 1) { userRepository.save(savedUser.captured) }
        Assertions.assertEquals(loggedInUserResponse.refreshToken, refreshTokenId)
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
                isConfirmed = true
        )
    }


}
