package com.qlarr.backend.mappers

import com.qlarr.backend.api.survey.*
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.persistence.entities.OfflineSurveyResponseCount
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.backend.persistence.entities.SurveyNavigationData
import com.qlarr.backend.persistence.entities.SurveyResponseCount
import org.springframework.stereotype.Component

@Component
class SurveyMapper(
    private val versionMapper: VersionMapper
) {

    fun mapCreateRequestToEntity(surveyCreateRequest: SurveyCreateRequest): SurveyEntity {
        return SurveyEntity(
            name = surveyCreateRequest.name.trim(),
            status = Status.DRAFT,
            usage = surveyCreateRequest.usage,
            quota = UNLIMITED_QUOTA,
            description = null,
            image = null,
            startDate = null,
            endDate = null,
            canLockSurvey = true,
            creationDate = nowUtc(),
            lastModified = nowUtc(),
            navigationData = SurveyNavigationData()
        )
    }

    fun mapEntityToDto(surveyEntity: SurveyEntity): SurveyDTO {
        return SurveyDTO(
            id = surveyEntity.id!!,
            creationDate = surveyEntity.creationDate!!,
            lastModified = surveyEntity.lastModified!!,
            startDate = surveyEntity.startDate,
            endDate = surveyEntity.endDate,
            name = surveyEntity.name,
            status = surveyEntity.status,
            usage = surveyEntity.usage,
            quota = surveyEntity.quota,
            description = surveyEntity.description,
            image = surveyEntity.image,
            canLockSurvey = surveyEntity.canLockSurvey,
            surveyNavigationData = surveyEntity.navigationData
        )
    }

    fun mapEntityToSimpleResponse(surveyResponseCount: SurveyResponseCount): SimpleSurveyDto {
        return SimpleSurveyDto(
            id = surveyResponseCount.survey.id!!,
            creationDate = surveyResponseCount.survey.creationDate!!,
            lastModified = surveyResponseCount.survey.lastModified!!,
            name = surveyResponseCount.survey.name,
            description = surveyResponseCount.survey.description,
            image = surveyResponseCount.survey.image,
            status = surveyResponseCount.survey.status,
            usage = surveyResponseCount.survey.usage,
            surveyQuota = surveyResponseCount.survey.quota,
            startDate = surveyResponseCount.survey.startDate,
            endDate = surveyResponseCount.survey.endDate,
            responsesCount = surveyResponseCount.responseCount.toInt(),
            completeResponseCount = surveyResponseCount.completeResponseCount.toInt(),
            latestVersion = versionMapper.toDto(
                surveyResponseCount.latestVersion,
                surveyStatus = surveyResponseCount.survey.status
            )
        )
    }

    fun mapEntityToOfflineResponse(surveyResponseCount: OfflineSurveyResponseCount): OfflineSurveyDto {
        return OfflineSurveyDto(
            id = surveyResponseCount.survey.id!!,
            creationDate = surveyResponseCount.survey.creationDate!!,
            lastModified = surveyResponseCount.survey.lastModified!!,
            name = surveyResponseCount.survey.name,
            description = surveyResponseCount.survey.description,
            image = surveyResponseCount.survey.image,
            status = surveyResponseCount.survey.status,
            usage = surveyResponseCount.survey.usage,
            surveyQuota = surveyResponseCount.survey.quota,
            userResponsesCount = surveyResponseCount.userResponseCount.toInt(),
            startDate = surveyResponseCount.survey.startDate,
            endDate = surveyResponseCount.survey.endDate,
            completeResponseCount = surveyResponseCount.completeResponseCount.toInt(),
            latestVersion = versionMapper.toDto(
                surveyResponseCount.latestVersion,
                surveyStatus = surveyResponseCount.survey.status
            ),
            navigationData = surveyResponseCount.survey.navigationData
        )
    }

    companion object {
        private const val UNLIMITED_QUOTA = -1
    }
}
