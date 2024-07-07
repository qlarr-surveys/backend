package com.frankie.backend.services

import com.frankie.backend.api.survey.Status
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.*
import com.frankie.backend.expressionmanager.SurveyProcessor
import com.frankie.backend.expressionmanager.validateSchema
import com.frankie.backend.persistence.entities.SurveyEntity
import com.frankie.backend.persistence.entities.SurveyResponseEntity
import com.frankie.backend.persistence.repositories.ResponseRepository
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.usecase.*
import org.springframework.stereotype.Service
import java.util.*

@Service
class NavigationService(
        private val responseRepository: ResponseRepository,
) {

    fun navigate(
            surveyId: UUID,
            response: SurveyResponseEntity?,
            navigationLang: String? = null,
            processedSurvey: ProcessedSurvey,
            navigationDirection: NavigationDirection,
            values: Map<String, Any> = mapOf(),
            preview: Boolean,
            surveyMode: SurveyMode
    ): NavigationResult {
        val surveyNavigationData = processedSurvey.validationJsonOutput.surveyNavigationData()
        val survey = processedSurvey.survey
        if (!preview && !survey.isActive()) {
            throw SurveyIsNotActiveException()
        } else if (!processedSurvey.latestVersion.valid) {
            throw SurveyDesignWithErrorException
        } else if (!surveyNavigationData.allowIncomplete && navigationDirection is NavigationDirection.Resume) {
            throw ResumeNotAllowed()
        } else if (!surveyNavigationData.allowJump && navigationDirection is NavigationDirection.Jump) {
            throw JumpNotAllowed()
        } else if (!surveyNavigationData.allowPrevious && navigationDirection is NavigationDirection.Previous) {
            throw PreviousNotAllowed()
        }
        val completeSurveyCount = responseRepository.completedSurveyCount(surveyId)
        validateSurveyForNavigation(survey, completeSurveyCount, preview)
        values.validateSchema(processedSurvey.validationJsonOutput.schema)
        val lang = response?.lang?.let { responseLang ->
            processedSurvey.validationJsonOutput.survey.availableLangByCode(navigationLang ?: responseLang)
        } ?: processedSurvey.validationJsonOutput.survey.availableLangByCode(navigationLang)

        val navigationUseCaseInput = NavigationUseCaseInput(
                values = mutableMapOf<String, Any>().apply {
                    response?.values?.let { putAll(it) }
                    putAll(values)
                },
                navigationInfo = NavigationInfo(
                        navigationDirection = navigationDirection,
                        navigationIndex = response?.navigationIndex
                ),
                lang = lang.code
        )
        val navigationJsonOutput = SurveyProcessor.navigate(
                processedSurvey.validationJsonOutput,
                navigationUseCaseInput,
                surveyNavigationData.skipInvalid,
                surveyMode
        )
        val additionalLang =
                mutableListOf(processedSurvey.validationJsonOutput.survey.defaultSurveyLang()).apply {
                    addAll(
                            processedSurvey.validationJsonOutput.survey.additionalLang()
                    )
                }.filter {
                    it.code != lang.code
                }
        return NavigationResult(
                navigationJsonOutput = navigationJsonOutput,
                lang = lang,
                additionalLang = additionalLang
        )

    }


    private fun validateSurveyForNavigation(survey: SurveyEntity, completeSurveyCount: Int, preview: Boolean) {
        if (survey.status == Status.CLOSED) {
            throw SurveyIsClosedException()
        }
        if (preview)
            return

        val now = nowUtc()
        survey.startDate?.let { startDate ->
            if (now.isBefore(startDate)) {
                throw SurveyNotStartedException(startDate)

            }
        }
        survey.endDate?.let { endDate ->
            if (now.isAfter(endDate)) {
                throw SurveyExpiredException()
            }
        }
        if (survey.quota >= 1 && survey.quota >= completeSurveyCount) {
            throw SurveyQuotaExceeded()
        }
    }
}

data class NavigationResult(
        val navigationJsonOutput: NavigationJsonOutput,
        val lang: SurveyLang,
        val additionalLang: List<SurveyLang>
)