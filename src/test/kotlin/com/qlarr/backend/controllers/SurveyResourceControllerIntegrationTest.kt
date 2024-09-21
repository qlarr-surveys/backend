package com.qlarr.backend.controllers

import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.api.user.Roles
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.exceptions.InvalidInputException
import com.qlarr.backend.exceptions.SurveyNotFoundException
import com.qlarr.backend.services.SurveyResourceService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*


@ContextConfiguration(classes = [SurveyResourceController::class])
class SurveyResourceControllerIntegrationTest : IntegrationTestBase() {


    @MockkBean
    private lateinit var surveyResourceService: SurveyResourceService
    private val file = MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".toByteArray())


    @Test
    fun `upload resource failed expect survey not found 404`() {
        every { surveyResourceService.uploadResource(any(), any()) } throws SurveyNotFoundException()

        mockMvc.perform(
            multipart("/survey/{surveyId}/resource", UUID.randomUUID()).file(file)
                .content(MediaType.MULTIPART_FORM_DATA_VALUE)
                .with(csrf())
                .withRole(Roles.SURVEY_ADMIN)
        ).andExpect(status().isNotFound).andExpect(jsonPath("$.message").value("Survey not found"))
    }

    @Disabled("Security rules were changed for demo purposes")
    @Test
    fun `upload resource failed expect unAuthorized 401`() {
        mockMvc.perform(
            multipart("/survey/{surveyId}/resource", UUID.randomUUID()).file(file)
                .content(MediaType.MULTIPART_FORM_DATA_VALUE)
                .with(csrf())
                .withRole(Roles.SURVEYOR)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `upload resource failed expect forbidden 400`() {
        val surveyId = UUID.randomUUID()
        every { surveyResourceService.uploadResource(any(), any()) } throws InvalidInputException()
        mockMvc.perform(
            multipart("/survey/{surveyId}/resource", surveyId).file(file).content(MediaType.MULTIPART_FORM_DATA_VALUE)
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid input")))
    }

    @Test
    fun `upload resource invalid inputs expect forbidden 400`() {
        mockMvc.perform(
            multipart("/survey/YYYYYY/resource").file(file).content(MediaType.MULTIPART_FORM_DATA_VALUE)
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.message").value(containsString("Invalid input")))
            .andReturn()
    }

    @Test
    fun `upload resource required part file expect forbidden 400`() {
        mockMvc.perform(
            multipart("/survey/{surveyId}/resource", UUID.randomUUID()).content(MediaType.MULTIPART_FORM_DATA_VALUE)
                .with(csrf())
                .withRole(Roles.SURVEY_ADMIN)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `upload resource expect succeed 200`() {
        val surveyId = UUID.randomUUID()
        every { surveyResourceService.uploadResource(any(), any()) } returns ResponseEntity.ok()
            .body(createRandomUploadResource())

        mockMvc.perform(
            multipart("/survey/{surveyId}/resource", surveyId).file(file).content(MediaType.MULTIPART_FORM_DATA_VALUE)
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
        ).andExpect(status().isOk)
    }


    @Test
    fun `download resource failed expect 400`() {
        every { surveyResourceService.downloadResource(any(), any(), any()) } throws InvalidInputException()
        mockMvc.perform(
            get("/survey/{surveyId}/resource/{fileName}", UUID.randomUUID(), "hello.txt")
                .with(csrf())
                .withRole(Roles.SURVEY_ADMIN)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid input")))
    }

    @Test
    fun `download resource failed expect 401`() {
        mockMvc.perform(
            get("/survey/{surveyId}/resource/{fileName}", UUID.randomUUID(), "hello.txt")
                .with(csrf())
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `download resource succeed expect 200`() {
        val surveyId = UUID.randomUUID()
        val dummyData = "This is a dummy file for testing.".toByteArray()
        val inputStream: InputStream = ByteArrayInputStream(dummyData)
        val resource = InputStreamResource(inputStream)

        every { surveyResourceService.downloadResource(any(), any(), any()) } returns ResponseEntity.ok().body(resource)

        mockMvc.perform(
            get("/survey/{surveyId}/resource/{fileName}", surveyId, "hello.txt")
                .with(csrf())
                .withRole(Roles.SURVEY_ADMIN)
        ).andExpect(status().isOk)
    }


    @Test
    fun `delete resource failed expect survey not found 404`() {
        every { surveyResourceService.removeResource(any(), any()) } throws SurveyNotFoundException()
        mockMvc.perform(
            delete("/survey/{surveyId}/resource/{fileName}", UUID.randomUUID(), "hello.txt")
                .with(csrf())
                .withRole(Roles.SURVEY_ADMIN)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Survey not found"))
    }

    @Test
    fun `delete resource failed expect 400`() {
        every { surveyResourceService.removeResource(any(), any()) } throws InvalidInputException()
        mockMvc.perform(
            delete("/survey/{surveyId}/resource/{fileName}", UUID.randomUUID(), "hello.txt")
                .with(csrf())
                .withRole(Roles.SURVEY_ADMIN)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid input")))
    }

    @Test
    fun `delete resource failed expect 403`() {
        mockMvc.perform(
            delete("/survey/{surveyId}/resource/{filename}", UUID.randomUUID(), "hello.txt")
                .with(user("admin").authorities(SimpleGrantedAuthority("role_admin")))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `delete resource failed expect 401`() {
        mockMvc.perform(
            delete("/survey/{surveyId}/resource/{fileName}", UUID.randomUUID(), "hello.txt")
                .with(csrf())
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `delete resource succeed expect 200`() {
        val surveyId = UUID.randomUUID()
        val byte = ByteArray(10)
        every { surveyResourceService.removeResource(any(), any()) } returns ResponseEntity.ok().body(byte)

        mockMvc.perform(
            delete("/survey/{surveyId}/resource/{fileName}", surveyId, "hello.txt")
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
        ).andExpect(status().isOk)
    }


    private fun createRandomUploadResource(): FileInfo = FileInfo(
        name = file.originalFilename, size = file.size, lastModified = nowUtc()
    )

}
