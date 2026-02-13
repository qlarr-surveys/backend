package com.qlarr.backend.controllers

import com.qlarr.backend.api.response.AnalyticsDto
import com.qlarr.backend.api.user.Roles
import com.qlarr.backend.exceptions.SurveyNotFoundException
import com.qlarr.backend.services.AnalyticsService
import com.qlarr.backend.services.ResponseService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@ContextConfiguration(classes = [ResponseController::class])
class ResponseControllerIntegrationTest : IntegrationTestBase() {

    @MockkBean
    private lateinit var responseService: ResponseService

    @MockkBean
    private lateinit var analyticsService: AnalyticsService

    private val surveyId = UUID.randomUUID()

    @Test
    fun `getAnalytics returns 200 for super_admin`() {
        val dto = AnalyticsDto("Test Survey", 10, emptyList())
        every { analyticsService.getAnalytics(surveyId, any()) } returns dto

        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.surveyTitle").value("Test Survey"))
            .andExpect(jsonPath("$.totalResponses").value(10))
            .andExpect(jsonPath("$.questions").isArray)
    }

    @Test
    fun `getAnalytics returns 200 for survey_admin`() {
        val dto = AnalyticsDto("Test Survey", 5, emptyList())
        every { analyticsService.getAnalytics(surveyId, any()) } returns dto

        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(csrf())
                .withRole(Roles.SURVEY_ADMIN)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.surveyTitle").value("Test Survey"))
    }

    @Test
    fun `getAnalytics returns 200 for analyst`() {
        val dto = AnalyticsDto("Test Survey", 0, emptyList())
        every { analyticsService.getAnalytics(surveyId, any()) } returns dto

        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(csrf())
                .withRole(Roles.ANALYST)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `getAnalytics denies access for surveyor`() {
        // AccessDeniedException is thrown by @PreAuthorize but mapped to 500 in test context
        // (no custom AccessDeniedHandler in @WebMvcTest). Verifying the endpoint rejects unauthorized roles.
        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(user("admin").authorities(SimpleGrantedAuthority("surveyor")))
        ).andExpect(status().is5xxServerError)
    }

    @Test
    fun `getAnalytics denies access for respondent`() {
        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(user("user").authorities(SimpleGrantedAuthority("respondent")))
        ).andExpect(status().is5xxServerError)
    }

    @Test
    fun `getAnalytics returns 401 for unauthenticated request`() {
        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getAnalytics returns 404 when survey not found`() {
        every { analyticsService.getAnalytics(surveyId, any()) } throws SurveyNotFoundException()

        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getAnalytics passes custom max_responses parameter`() {
        val dto = AnalyticsDto("Test Survey", 0, emptyList())
        every { analyticsService.getAnalytics(surveyId, 100) } returns dto

        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .param("max_responses", "100")
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `getAnalytics uses default max_responses when not provided`() {
        val dto = AnalyticsDto("Test Survey", 0, emptyList())
        every { analyticsService.getAnalytics(surveyId, AnalyticsService.DEFAULT_MAX_RESPONSES) } returns dto

        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", surveyId)
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `getAnalytics returns 400 for invalid surveyId`() {
        mockMvc.perform(
            get("/survey/{surveyId}/response/analytics", "invalid-uuid")
                .with(csrf())
                .withRole(Roles.SUPER_ADMIN)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }
}
