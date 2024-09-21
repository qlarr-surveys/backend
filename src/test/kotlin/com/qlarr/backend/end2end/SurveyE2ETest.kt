package com.qlarr.backend.end2end

import com.qlarr.backend.api.survey.SurveyCreateRequest
import com.qlarr.backend.api.survey.SurveyDTO
import com.qlarr.backend.api.survey.Usage
import com.qlarr.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.qlarr.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class SurveyE2ETest : E2ETestBase() {


    @Test
    fun create_survey_expect_succeed() {
        val adminEmail = "admin@admin.admin"
        val adminPassword = "admin"
        val authToken = login(adminEmail, adminPassword)

        val surveyCreateRequest = SurveyCreateRequest(
            name = "new_survey",
            usage = Usage.WEB
        )

        webTestClient.post()
            .uri("/survey/create")
            .header(HEADER_STRING, TOKEN_PREFIX + authToken)
            .bodyValue(surveyCreateRequest)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(SurveyDTO::class.java)
            .consumeWith(System.out::println)
            .returnResult()
    }

}
