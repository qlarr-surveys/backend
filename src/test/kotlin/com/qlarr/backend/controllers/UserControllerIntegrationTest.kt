package com.qlarr.backend.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qlarr.backend.api.user.*
import com.qlarr.backend.exceptions.DuplicateEmailException
import com.qlarr.backend.exceptions.UserNotFoundException
import com.qlarr.backend.services.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@ContextConfiguration(classes = [UserController::class])
class UserControllerIntegrationTest : IntegrationTestBase() {


    @MockkBean
    private lateinit var userService: UserService
    private val objectMapper = jacksonObjectMapper()


    @Test
    fun test_getAll_unfilteredEmpty_expect_emptyResult() {
        every { userService.getAllUsers() } returns emptyList()

        mockMvc.perform(
                get("/user/all")
                        .with(csrf()).withRole(Roles.SUPER_ADMIN)
        )
                .andDo(print())
                .andExpect(status().isOk)
    }

    @Test
    fun test_getAll_unfiltered_expect_singleResult() {
        val userDTO = createRandomUserDTO()
        val userList = listOf(userDTO)
        every { userService.getAllUsers() } returns userList

        mockMvc.perform(get("/user/all").withRole(Roles.SUPER_ADMIN))
                .andDo(print())
                .andExpect(status().isOk)
    }

    @Test
    @WithAnonymousUser
    fun test_getAll_failed_expect_unauthorized() {
        mockMvc.perform(get("/user/all"))
                .andDo(print())
                .andExpect(status().isUnauthorized)
    }


    @Test
    fun `test getById unknown userId expect userNotFound`() {
        val surveyId: UUID = UUID.fromString("d2af324d-4431-4d06-ae4f-ef21f98f247e")
        every { userService.getUserById(any()) } throws UserNotFoundException()

        mockMvc.perform(get("/user/{userId}", surveyId).withRole(Roles.SUPER_ADMIN))
                .andDo(print())
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("User not found"))
    }

    @Test
    fun `test getById invalidUserId expect invalidRequest`() {
        mockMvc.perform(get("/user/wrongId").withRole(Roles.SURVEYOR))
                .andDo(print())
                .andExpect(status().isBadRequest)
    }

    @Test
    @WithAnonymousUser
    fun `test getById validUserId expect unAuthorized`() {
        mockMvc.perform(get("/user/{userId}", UUID.randomUUID()).with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized)
    }

    @Test
    fun test_getById_valid_expect_result() {
        val userRequestDTO = createRandomUserDTO()
        val userId = userRequestDTO.id
        every { userService.getUserById(userId) } returns userRequestDTO

        mockMvc.perform(get("/user/{userId}", userId).withRole(Roles.SUPER_ADMIN))
                .andDo(print())
                .andExpect(status().isOk)
    }


    @Test
    fun test_create_valid_authorized_expect_result() {
        val createRequest = createRandomCreateRequest()
        val userDTO = createRandomUserDTO()
        every { userService.create(createRequest) } returns userDTO

        mockMvc.perform(
                post("/user/create")
                        .with(csrf())
                        .withRole(Roles.SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapCreateRequestToPayload(createRequest))
        )
                .andDo(print())
                .andExpect(status().isOk)
    }

    @Test
    fun test_create_failed_expect_unAuthorized() {
        val createRequest = createRandomCreateRequest()
        mockMvc.perform(
                post("/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapCreateRequestToPayload(createRequest))
        )
                .andExpect(status().isForbidden)
    }

    @Test
    fun `test create duplicateEmail expect invalidInput`() {
        val createRequest = createRandomCreateRequest()
        every { userService.create(createRequest) } throws DuplicateEmailException()

        mockMvc.perform(
                post("/user/create")
                        .with(csrf()).withRole(Roles.SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapCreateRequestToPayload(createRequest))
        )
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Invalid input, duplicate email"))
    }
    // TODO test.txt invalid create request object


    @Test
    fun `test edit noChanges expect sameResult`() {
        val userId = UUID.randomUUID()
        val ed = createRandomEditRequest()
        val userDTO = createRandomUserDTO()
        every { userService.update(userId, ed) } returns userDTO

        mockMvc.perform(
                put("/user/{userId}", userId)
                        .with(csrf()).withRole(Roles.SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ed))
        )
                .andDo(print())
                .andExpect(status().isOk)
    }

    @Test
    fun `test edit unknown userId expect userNotFound`() {
        val userId = UUID.randomUUID()
        val createRequest = createRandomCreateRequest()
        every { userService.update(any(), any()) } throws UserNotFoundException()

        mockMvc.perform(
                put("/user/{userId}", userId)
                        .with(csrf()).withRole(Roles.SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapCreateRequestToPayload(createRequest))
        )
                .andDo(print())
                .andExpect(status().isNotFound)
    }

    @Test
    fun `test edit invalid userId expect invalidInput`() {
        val createRequest = createRandomCreateRequest()

        mockMvc.perform(
                put("/user/wrongId")
                        .with(csrf()).withRole(Roles.SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapCreateRequestToPayload(createRequest))
        )
                .andDo(print())
                .andExpect(status().isBadRequest)
    }

    @Test
    @WithAnonymousUser
    fun `test edit failed expect unauthorized`() {
        val createRequest = createRandomCreateRequest()
        mockMvc.perform(
                put("/user/{userId}", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapCreateRequestToPayload(createRequest))
        )
                .andDo(print())
                .andExpect(status().isUnauthorized)
    }


    @Test
    fun `test delete existing expect success`() {
        justRun { userService.delete(any()) }

        mockMvc.perform(delete("/user/{userId}", UUID.randomUUID()).with(csrf()).withRole(Roles.SUPER_ADMIN))
                .andDo(print())
                .andExpect(status().isNoContent)
    }

    @Test
    fun `test delete unknown expect notFound`() {
        val userId = UUID.randomUUID()
        every { userService.delete(any()) } throws UserNotFoundException()

        mockMvc.perform(
                delete("/user/{userId}", userId)
                        .with(csrf())
                        .withRole(Roles.SUPER_ADMIN)
        )
                .andDo(print())
                .andExpect(status().isNotFound)
    }

    @Test
    fun `test delete invalidInput expect bad-request`() {
        mockMvc.perform(delete("/user/wrongId").with(csrf()).withRole(Roles.SUPER_ADMIN))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value(containsString("Invalid input")))
    }

    @Test
    @WithAnonymousUser
    fun `test delete failed expect UnAuthorized`() {
        mockMvc.perform(
                delete("/user/{userId}", UUID.randomUUID())
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized)
    }


    private fun createRandomUserDTO(): UserDTO = UserDTO(
            UUID.randomUUID(),
            "firstName-" + UUID.randomUUID(),
            "lastName-" + UUID.randomUUID(),
            "email-" + UUID.randomUUID(),
            true,
            setOf(),
            )

    private fun createRandomCreateRequest(): CreateRequest = CreateRequest(
            firstName = "firstName-" + UUID.randomUUID(),
            lastName = "lastName-" + UUID.randomUUID(),
            email = "email-" + UUID.randomUUID(),
            roles = emptySet(),
    )

    private fun createRandomEditRequest() = EditUserRequest(
            firstName = "firstName-" + UUID.randomUUID(),
            lastName = "lastName-" + UUID.randomUUID(),
            roles = emptySet(),
    )

    @Test
    @WithAnonymousUser
    fun test_getUserCountByRole_failed_expect_unAuthorized() {
        val mockedResponse: CountByRoleResponse = mockk()
        every { userService.countUserRoles() } returns mockedResponse
        every { mockedResponse.superAdmin } returns 1
        every { mockedResponse.surveyor } returns 2
        every { mockedResponse.surveyAdmin } returns 3

        val result = mockMvc.perform(get("/user/count_by_role"))
                .andDo(print())
                .andExpect(status().isUnauthorized)
                .andReturn()

        Assertions.assertThat(result.response.contentAsString).isEmpty()
        Assertions.assertThat(result.response.errorMessage).isEqualTo("Unauthorized")
        Assertions.assertThat(result.response.redirectedUrl).isNull()

        verify(exactly = 0) { userService.countUserRoles() }
    }

    @Test
    fun test_getUserCountByRole_anyValues_expect_validResult() {
        val mockedResponse: CountByRoleResponse = mockk()
        every { userService.countUserRoles() } returns mockedResponse
        every { mockedResponse.superAdmin } returns 1
        every { mockedResponse.surveyor } returns 2
        every { mockedResponse.surveyAdmin } returns 3

        val result = mockMvc.perform(
                get("/user/count_by_role").withRole(Roles.SUPER_ADMIN)
        )
                .andDo(print())
                .andExpect(status().isOk)
                .andReturn()

        Assertions.assertThat(result.response.contentAsString).contains("\"superAdmin\":1")
        Assertions.assertThat(result.response.contentAsString).contains("\"surveyAdmin\":3")
        Assertions.assertThat(result.response.contentAsString).contains("\"surveyor\":2")

        verify(exactly = 1) { userService.countUserRoles() }
    }

    private fun mapCreateRequestToPayload(createRequest: CreateRequest): String =
            objectMapper.writeValueAsString(createRequest)

}
