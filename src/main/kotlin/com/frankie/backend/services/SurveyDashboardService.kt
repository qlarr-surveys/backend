package com.frankie.backend.services

import com.frankie.backend.api.survey.OfflineSurveyDto
import com.frankie.backend.api.survey.SurveysDto
import com.frankie.backend.common.UserUtils
import com.frankie.backend.mappers.SurveyMapper
import com.frankie.backend.persistence.repositories.SurveyFilter
import com.frankie.backend.persistence.repositories.SurveyRepository
import com.frankie.backend.persistence.repositories.SurveySort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class SurveyDashboardService(
        private val surveyMapper: SurveyMapper,
        private val surveyRepository: SurveyRepository,
        private val userUtils: UserUtils

) {
    fun getAllSurveys(page: Int?, perPage: Int?, sortBy: String?, status: String?): SurveysDto {
        val surveySort = SurveySort.parse(sortBy)
        val surveyFilter = SurveyFilter.parse(status)
        val pageable = PageRequest.of(
                page?.minus(1) ?: 0,
                perPage ?: 5
        )

        val pages = if (surveySort == SurveySort.LAST_MODIFIED_DESC) {
            surveyRepository.findAllSurveysSortByLastModified(
                    active = surveyFilter == SurveyFilter.ACTIVE,
                    scheduled = surveyFilter == SurveyFilter.SCHEDULED,
                    expired = surveyFilter == SurveyFilter.EXPIRED,
                    status = surveyFilter.status,
                    pageable = pageable)
        } else {
            surveyRepository.findAllSurveysSortByResponses(
                    active = surveyFilter == SurveyFilter.ACTIVE,
                    scheduled = surveyFilter == SurveyFilter.SCHEDULED,
                    expired = surveyFilter == SurveyFilter.EXPIRED,
                    status = surveyFilter.status,
                    pageable = pageable)
        }
        return SurveysDto(
                totalCount = pages.totalElements.toInt(),
                totalPages = pages.totalPages,
                pageNumber = pages.number,
                surveys = pages.content.map { surveyMapper.mapEntityToSimpleResponse(it) }
        )
    }

    fun surveysForOffline(): List<OfflineSurveyDto> {
        return surveyRepository.findAllOfflineSurveysByUserId(userUtils.currentUserId())
                .map(surveyMapper::mapEntityToOfflineResponse)

    }
}
