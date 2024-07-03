package com.frankie.backend.end2end

import com.frankie.backend.api.design.DesignDto
import com.frankie.backend.api.runsurvey.NavigateRequest
import com.frankie.backend.api.runsurvey.RunSurveyDto
import com.frankie.backend.api.runsurvey.StartRequest
import com.frankie.backend.common.nowUtc
import com.frankie.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.frankie.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import com.frankie.expressionmanager.model.NavigationDirection
import com.frankie.expressionmanager.model.NavigationIndex
import com.frankie.expressionmanager.model.ResponseEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.util.*

@Suppress("UNCHECKED_CAST")
class RunSurveyE2ETest : E2ETestBase() {


    @Test
    fun create_survey_expect_succeed() {
        val adminEmail = "email" + UUID.randomUUID() + "@koko.com"
        val adminPassword = "password"
        val domain = UUID.randomUUID().toString().lowercase().take(12)
        createTenant(adminEmail, adminPassword, domain)
        val authToken = login(adminEmail, adminPassword)

        val designDto = webTestClient.post()
            .uri("/survey/sample_survey_design")
            .header(HEADER_STRING, TOKEN_PREFIX + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(DesignDto::class.java)
            .consumeWith(System.out::println)
            .returnResult()
            .responseBody as DesignDto
        // I know it is cheap...
        // but we save design to S3 in async mannaer
        Thread.sleep(500L)

        val startRequest = StartRequest(
        )

        val runSurveyDto = webTestClient.post()
            .uri("/survey/${designDto.versionDto.surveyId}/preview/start")
            .header(HEADER_STRING, TOKEN_PREFIX + authToken)
            .bodyValue(startRequest)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(RunSurveyDto::class.java)
            .consumeWith(System.out::println)
            .returnResult()
            .responseBody as RunSurveyDto

        val navigateRequest = NavigateRequest(
            responseId = runSurveyDto.responseId,
            events = listOf(ResponseEvent.Value("Q1", nowUtc())),
            navigationDirection = NavigationDirection.Next
        )

        val navSurveyDto = webTestClient.post()
            .uri("/survey/${designDto.versionDto.surveyId}/preview/navigate")
            .header(HEADER_STRING, TOKEN_PREFIX + authToken)
            .bodyValue(navigateRequest)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(RunSurveyDto::class.java)
            .consumeWith(System.out::println)
            .returnResult()
            .responseBody as RunSurveyDto

        assert(runSurveyDto.navigationIndex is NavigationIndex.Group)
        assert(navSurveyDto.navigationIndex is NavigationIndex.Group)
        Assertions.assertNotEquals(
            (runSurveyDto.navigationIndex as NavigationIndex.Group).groupId,
            (navSurveyDto.navigationIndex as NavigationIndex.Group).groupId
        )

        val responseId = runSurveyDto.responseId

        webTestClient.get()
            .uri("/survey/${designDto.versionDto.surveyId}/response/$responseId/events")
            .header(HEADER_STRING, TOKEN_PREFIX + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(List::class.java)
            .consumeWith(System.out::println)
            .returnResult()
            .responseBody as List<ResponseEvent>
    }

}
