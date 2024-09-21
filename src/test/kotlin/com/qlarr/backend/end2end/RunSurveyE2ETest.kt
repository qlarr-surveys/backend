package com.qlarr.backend.end2end

import com.qlarr.backend.api.response.ResponsesDto
import com.qlarr.backend.api.runsurvey.NavigateRequest
import com.qlarr.backend.api.runsurvey.RunSurveyDto
import com.qlarr.backend.api.runsurvey.StartRequest
import com.qlarr.backend.api.survey.SurveyCreateRequest
import com.qlarr.backend.api.survey.SurveyDTO
import com.qlarr.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.qlarr.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import com.qlarr.expressionmanager.model.NavigationDirection
import com.qlarr.expressionmanager.model.NavigationIndex
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class RunSurveyE2ETest : E2ETestBase() {


    @Test
    fun create_survey_expect_succeed() {
        val adminEmail = "admin@admin.admin"
        val adminPassword = "admin"
        val authToken = login(adminEmail, adminPassword)

        val surveyDTO = webTestClient.post()
                .uri("/survey/create")
                .header(HEADER_STRING, TOKEN_PREFIX + authToken)
                .bodyValue(SurveyCreateRequest("Kabaka"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody(SurveyDTO::class.java)
                .consumeWith(System.out::println)
                .returnResult()
                .responseBody as SurveyDTO
        // I know it is cheap...
        // but we save design to S3 in async mannaer
        Thread.sleep(500L)

        val startRequest = StartRequest(
        )

        val runSurveyDto = webTestClient.post()
                .uri("/survey/${surveyDTO.id}/preview/start")
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
                navigationDirection = NavigationDirection.Next
        )

        val navSurveyDto = webTestClient.post()
                .uri("/survey/${surveyDTO.id}/preview/navigate")
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
        Assertions.assertNotEquals(runSurveyDto.navigationIndex, navSurveyDto.navigationIndex)

        webTestClient.get()
                .uri("/survey/${surveyDTO.id}/response/all")
                .header(HEADER_STRING, TOKEN_PREFIX + authToken)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody(ResponsesDto::class.java)
                .consumeWith(System.out::println)
                .returnResult()
                .responseBody
    }

}
