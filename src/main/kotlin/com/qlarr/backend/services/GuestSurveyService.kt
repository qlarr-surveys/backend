package com.qlarr.backend.services

import com.qlarr.backend.api.survey.CloneRequest
import com.qlarr.backend.api.survey.SurveyDTO
import com.qlarr.backend.helpers.FileSystemHelper
import com.qlarr.backend.mappers.SurveyMapper
import com.qlarr.backend.persistence.repositories.SurveyRepository
import com.qlarr.backend.persistence.repositories.VersionRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.util.*


@Service
class GuestSurveyService(
    val restTemplate: RestTemplate,
    val surveyService: SurveyService,
    @Value("\${enterprise.domain}") val enterpriseDomain: String
) {

    @Transactional
    fun cloneGuestSurvey(surveyId: UUID, cloneRequest: CloneRequest): SurveyDTO {
        val url = "$enterpriseDomain/guest/survey/$surveyId/export"
        val response = restTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            throw ResponseStatusException(response.statusCode, "Cloning guest survey to local failed")
        }

        return surveyService.importSurvey(cloneRequest.name,response.body!!.inputStream())
    }
}