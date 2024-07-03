package com.frankie.backend.services

import com.frankie.backend.common.UserUtils
import com.frankie.backend.exceptions.AuthorizationException
import com.frankie.backend.exceptions.PermissionAlreadyExists
import com.frankie.backend.exceptions.PermissionNotFoundException
import com.frankie.backend.mappers.UserMapper
import com.frankie.backend.persistence.entities.PermissionEntity
import com.frankie.backend.persistence.entities.PermissionId
import com.frankie.backend.persistence.repositories.SurveyRepository
import com.frankie.backend.persistence.repositories.UserRepository
import com.frankie.backend.services.SurveyServiceTest.Companion.buildSurvey
import com.frankie.backend.services.UserServiceTest.Companion.generateSurveyor
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import jakarta.persistence.*
import org.junit.Assert.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

@ExtendWith(MockKExtension::class)
class PermissionServiceTest {

    private val userMapper = UserMapper(BCryptPasswordEncoder())
    private val permissionMapper = PermissionMapper()

    @MockK
    private lateinit var permissionRepository: PermissionRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var userUtils: UserUtils

    @MockK
    private lateinit var surveyRepository: SurveyRepository

    @InjectMockKs
    private lateinit var permissionService: PermissionService


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }


    @Test
    fun `cannot add without permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        hasPermission(adminID, surveyId, false)
        assertThrows(AuthorizationException::class.java) {
            permissionService.add(surveyId, UUID.randomUUID())
        }
    }

    @Test
    fun `can add with permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val permission = PermissionEntity(userId, surveyId)
        hasPermission(adminID, surveyId, true)
        every { surveyRepository.findByIdOrNull(surveyId) } returns buildSurvey(surveyId)
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns generateSurveyor(userId)
        every { permissionRepository.save(permission) } returns permission
        val response = permissionService.add(surveyId, userId)
        Assertions.assertEquals(response.userId, userId)
        Assertions.assertEquals(response.surveyId, surveyId)
    }

    @Test
    fun `adding an already existing permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val permission = PermissionEntity(userId, surveyId)
        hasPermission(adminID, surveyId, true)
        every { surveyRepository.findByIdOrNull(surveyId) } returns buildSurvey(surveyId)
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns generateSurveyor(userId)
        every { permissionRepository.save(permission) } throws DataIntegrityViolationException("")
        assertThrows(PermissionAlreadyExists::class.java) {
            permissionService.add(surveyId, userId)
        }
    }


    @Test
    fun `cannot getAllUsers without permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        hasPermission(adminID, surveyId, false)
        assertThrows(AuthorizationException::class.java) {
            permissionService.getAllUsers(surveyId)
        }
    }


    @Test
    fun `cannot getPermission without permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        hasPermission(adminID, surveyId, false)
        assertThrows(AuthorizationException::class.java) {
            permissionService.getPermission(surveyId, UUID.randomUUID())
        }
    }

    @Test
    fun `can getPermission with permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val permission = PermissionEntity(userId, surveyId)
        hasPermission(adminID, surveyId, true)
        every { permissionRepository.findByIdOrNull(PermissionId(userId, surveyId)) } returns permission
        val returnPermission = permissionService.getPermission(surveyId, userId)
        Assertions.assertEquals(permission.surveyId, returnPermission.surveyId)
        Assertions.assertEquals(permission.userId, returnPermission.userId)
    }

    @Test
    fun `getPermission id not found throws permission not found`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        hasPermission(adminID, surveyId, true)
        every { permissionRepository.findByIdOrNull(PermissionId(userId, surveyId)) } returns null
        assertThrows(PermissionNotFoundException::class.java) {
            permissionService.getPermission(surveyId, userId)
        }
    }

    @Test
    fun `cannot removePermission without permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        hasPermission(adminID, surveyId, false)
        assertThrows(AuthorizationException::class.java) {
            permissionService.removePermission(surveyId, UUID.randomUUID())
        }
    }

    @Test
    fun `can removePermission with permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val deletedPermission = slot<PermissionEntity>()
        val permission = PermissionEntity(userId, surveyId)
        hasPermission(adminID, surveyId, true)
        every { permissionRepository.findByIdOrNull(PermissionId(userId, surveyId)) } returns permission
        justRun { permissionRepository.delete(capture(deletedPermission)) }
        permissionService.removePermission(surveyId, userId)
        Assertions.assertEquals(deletedPermission.captured.surveyId, surveyId)
        Assertions.assertEquals(deletedPermission.captured.userId, userId)
    }

    @Test
    fun `removePermission if not found throws permission not found`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        hasPermission(adminID, surveyId, true)
        every { permissionRepository.findByIdOrNull(PermissionId(userId, surveyId)) } returns null
        assertThrows(PermissionNotFoundException::class.java) {
            permissionService.removePermission(surveyId, userId)
        }
    }

    @Test
    fun `cannot update survey without permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        hasPermission(adminID, surveyId, false)
        assertThrows(AuthorizationException::class.java) {
            permissionService.update(surveyId, UUID.randomUUID(), 100)
        }
    }

    @Test
    fun `can update with permission`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val modifiedPermission = slot<PermissionEntity>()
        val permission = PermissionEntity(userId, surveyId)
        hasPermission(adminID, surveyId, true)
        every { permissionRepository.findByIdOrNull(PermissionId(userId, surveyId)) } returns permission
        every { permissionRepository.save(capture(modifiedPermission)) } returns permission
        permissionService.update(surveyId, userId, 100)
        Assertions.assertEquals(modifiedPermission.captured, permission.copy(quota = 100))
    }

    @Test
    fun `update if not found throws permission not found`() {
        val adminID = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        hasPermission(adminID, surveyId, true)
        every { permissionRepository.findByIdOrNull(PermissionId(userId, surveyId)) } returns null
        assertThrows(PermissionNotFoundException::class.java) {
            permissionService.update(surveyId, userId, 10000)
        }
    }

    private fun hasPermission(userId: UUID, surveyId: UUID, permission: Boolean) {
        every { userUtils.isSuperAdmin() } returns false
        every { userUtils.currentUserId() } returns userId
        every { permissionRepository.hasPermission(userId, surveyId) } returns permission
    }


}
