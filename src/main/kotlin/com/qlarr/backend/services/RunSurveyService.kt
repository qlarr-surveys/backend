package com.qlarr.backend.services

import com.qlarr.backend.api.runsurvey.NavigateRequest
import com.qlarr.backend.api.runsurvey.RunSurveyDto
import com.qlarr.backend.api.runsurvey.StartRequest
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.exceptions.ResponseNotFoundException
import com.qlarr.backend.exceptions.SurveyIsNotActiveException
import com.qlarr.backend.mappers.RunMapper
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.ext.commonScript
import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import com.qlarr.surveyengine.model.exposed.SurveyMode
import com.qlarr.surveyengine.usecase.SurveyDesignWithErrorException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RunSurveyService(
    private val designService: DesignService,
    private val navigationService: NavigationService,
    private val runMapper: RunMapper,
    private val responseRepository: ResponseRepository,
) {


    fun start(
        surveyId: UUID,
        startRequest: StartRequest,
        preview: Boolean,
        surveyMode: SurveyMode
    ): RunSurveyDto {
        val processedSurvey = designService.getProcessedSurvey(surveyId, !preview)

        val result = navigationService.navigate(
            surveyId = surveyId,
            response = null,
            processedSurvey = processedSurvey,
            navigationLang = startRequest.lang,
            navigationMode = startRequest.navigationMode,
            navigationDirection = NavigationDirection.Start,
            values = startRequest.values,
            preview = preview,
            surveyMode = surveyMode
        )

        val responseEntity = SurveyResponseEntity(
            surveyId = surveyId,
            lang = result.lang.code,
            values = result.navigationJsonOutput.toSave,
            startDate = nowUtc(),
            navigationIndex = result.navigationJsonOutput.navigationIndex,
            surveyor = null,
            preview = preview,
            version = processedSurvey.latestVersion.version
        )
        val savedResponse = responseRepository.save(responseEntity)

        return runMapper.toRunDto(
            savedResponse.id!!,
            result.lang,
            result.additionalLang,
            result.navigationJsonOutput,
            processedSurvey.survey
        )
    }

    fun navigate(
        surveyId: UUID,
        navigateRequest: NavigateRequest,
        preview: Boolean,
        surveyMode: SurveyMode
    ): RunSurveyDto {
        val processedSurvey = designService.getProcessedSurvey(surveyId, !preview)
        val response = responseRepository.findByIdOrNull(navigateRequest.responseId)
            ?: throw ResponseNotFoundException()
        val result = navigationService.navigate(
            surveyId = surveyId,
            response = response,
            navigationLang = navigateRequest.lang,
            processedSurvey = processedSurvey,
            navigationDirection = navigateRequest.navigationDirection,
            values = response.values.toMutableMap().apply {
                putAll(navigateRequest.values)
            },
            preview = preview,
            surveyMode = surveyMode
        )
        val entityToSave = response.copy(
            navigationIndex = result.navigationJsonOutput.navigationIndex,
            lang = result.lang.code,
            submitDate = if (result.navigationJsonOutput.navigationIndex is NavigationIndex.End) nowUtc() else null,
            values = result.navigationJsonOutput.toSave,
            preview = preview
        )
        responseRepository.save(entityToSave)
        return runMapper.toRunDto(
            navigateRequest.responseId,
            result.lang,
            result.additionalLang,
            result.navigationJsonOutput,
            processedSurvey.survey
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
        return processedSurvey.validationJsonOutput.script + "\n\n" + commonScript().script
    }
}