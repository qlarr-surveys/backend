package com.frankie.backend.services

import com.frankie.backend.api.runsurvey.NavigateRequest
import com.frankie.backend.api.runsurvey.RunSurveyDto
import com.frankie.backend.api.runsurvey.StartRequest
import com.frankie.backend.api.survey.Status
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.*
import com.frankie.backend.expressionmanager.SurveyProcessor
import com.frankie.backend.expressionmanager.validateSchema
import com.frankie.backend.mappers.RunMapper
import com.frankie.backend.persistence.entities.SurveyEntity
import com.frankie.backend.persistence.entities.SurveyResponseEntity
import com.frankie.backend.persistence.repositories.ResponseRepository
import com.frankie.expressionmanager.ext.ScriptUtils
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.usecase.SurveyDesignWithErrorException
import com.frankie.expressionmanager.usecase.additionalLang
import com.frankie.expressionmanager.usecase.availableLangByCode
import com.frankie.expressionmanager.usecase.defaultSurveyLang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RunSurveyService(
    private val designService: DesignService,
    private val runMapper: RunMapper,
    private val responseRepository: ResponseRepository,
) {
    fun start(
        ipAddress: String,
        surveyId: UUID,
        startRequest: StartRequest,
        preview: Boolean,
        surveyMode: SurveyMode
    ): RunSurveyDto {
        val processedSurvey = designService.getProcessedSurvey(surveyId, !preview)
        val surveyNavigationData = processedSurvey.validationJsonOutput.surveyNavigationData()
        val survey = processedSurvey.survey
        if (!preview && !survey.isActive()) {
            throw SurveyIsNotActiveException()
        }
        if (!processedSurvey.latestVersion.valid) {
            throw SurveyDesignWithErrorException
        }

        val completeSurveyCount = responseRepository.completedSurveyCount(surveyId)
        validateSurveyForNavigation(survey, completeSurveyCount, preview)
        val lang = processedSurvey.validationJsonOutput.survey.availableLangByCode(startRequest.lang)
        startRequest.values.validateSchema(processedSurvey.validationJsonOutput.schema)
        val navigationUseCaseInput = NavigationUseCaseInput(
            values = startRequest.values,
            navigationInfo = NavigationInfo(
                navigationDirection = NavigationDirection.Start,
                navigationIndex = null
            ),
            lang = lang.code
        )
        val result =
            SurveyProcessor.navigate(
                processedSurvey.validationJsonOutput,
                navigationUseCaseInput,
                surveyNavigationData.skipInvalid,
                surveyMode
            )
        val responseEntity = SurveyResponseEntity(
            surveyId = surveyId,
            lang = lang.code,
            values = result.toSave,
            startDate = nowUtc(),
            navigationIndex = result.navigationIndex,
            surveyor = null,
            preview = preview,
            version = processedSurvey.latestVersion.version
        )
        val savedResponse = responseRepository.save(responseEntity)
        val additionalLang =
            mutableListOf(processedSurvey.validationJsonOutput.survey.defaultSurveyLang()).apply {
                addAll(
                    processedSurvey.validationJsonOutput.survey.additionalLang()
                )
            }.filter {
                it.code != lang.code
            }
        return runMapper.toRunDto(
            savedResponse.id!!,
            lang,
            additionalLang,
            result,
            survey
        )
    }

    fun navigate(
        surveyId: UUID,
        navigateRequest: NavigateRequest,
        preview: Boolean,
        surveyMode: SurveyMode
    ): RunSurveyDto {
        val processedSurvey = designService.getProcessedSurvey(surveyId, !preview)
        val surveyNavigationData = processedSurvey.validationJsonOutput.surveyNavigationData()
        val survey = processedSurvey.survey
        if (!preview && !survey.isActive()) {
            throw SurveyIsNotActiveException()
        } else if (!processedSurvey.latestVersion.valid) {
            throw SurveyDesignWithErrorException
        } else if (!surveyNavigationData.allowIncomplete && navigateRequest.navigationDirection is NavigationDirection.Resume) {
            throw ResumeNotAllowed()
        } else if (!surveyNavigationData.allowJump && navigateRequest.navigationDirection is NavigationDirection.Jump) {
            throw JumpNotAllowed()
        } else if (!surveyNavigationData.allowPrevious && navigateRequest.navigationDirection is NavigationDirection.Previous) {
            throw PreviousNotAllowed()
        }
        val response =
            responseRepository.findByIdOrNull(navigateRequest.responseId) ?: throw ResponseNotFoundException()

        val completeSurveyCount = responseRepository.completedSurveyCount(surveyId)
        validateSurveyForNavigation(survey, completeSurveyCount, preview)
        val lang =
            processedSurvey.validationJsonOutput.survey.availableLangByCode(navigateRequest.lang ?: response.lang)
        val navigationUseCaseInput = NavigationUseCaseInput(
            values = response.values.toMutableMap().apply {
                putAll(navigateRequest.values)
            },
            navigationInfo = NavigationInfo(
                navigationDirection = navigateRequest.navigationDirection,
                navigationIndex = response.navigationIndex
            ),
            lang = lang.code,
        )
        val navigationResult =
            SurveyProcessor.navigate(
                processedSurvey.validationJsonOutput,
                navigationUseCaseInput,
                surveyNavigationData.skipInvalid,
                surveyMode
            )
        navigateRequest.values.validateSchema(processedSurvey.validationJsonOutput.schema)
        val entityToSave = response.copy(
            navigationIndex = navigationResult.navigationIndex,
            lang = lang.code,
            submitDate = if (navigationResult.navigationIndex is NavigationIndex.End) nowUtc() else null,
            values = navigationResult.toSave,
            preview = preview
        )
        responseRepository.save(entityToSave)
        val additionalLang =
            mutableListOf(processedSurvey.validationJsonOutput.survey.defaultSurveyLang()).apply {
                addAll(
                    processedSurvey.validationJsonOutput.survey.additionalLang()
                )
            }.filter {
                it.code != lang.code
            }
        return runMapper.toRunDto(
            navigateRequest.responseId,
            lang,
            additionalLang,
            navigationResult,
            survey
        )
    }

    fun runtimeJs(surveyId: UUID, preview: Boolean = false): String {
        val processedSurvey = designService.getProcessedSurvey(surveyId, !preview)
        if (!preview && !processedSurvey.survey.isActive()) {
            throw SurveyIsNotActiveException()
        }
        if (!processedSurvey.latestVersion.valid) {
            throw SurveyDesignWithErrorException
        }
        return processedSurvey.validationJsonOutput.script + "\n\n" + ScriptUtils().commonScript
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