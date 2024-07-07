package com.frankie.backend.services

import com.frankie.backend.api.runsurvey.NavigateRequest
import com.frankie.backend.api.runsurvey.RunSurveyDto
import com.frankie.backend.api.runsurvey.StartRequest
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.ResponseNotFoundException
import com.frankie.backend.exceptions.SurveyIsNotActiveException
import com.frankie.backend.mappers.RunMapper
import com.frankie.backend.persistence.entities.SurveyResponseEntity
import com.frankie.backend.persistence.repositories.ResponseRepository
import com.frankie.expressionmanager.ext.ScriptUtils
import com.frankie.expressionmanager.model.NavigationDirection
import com.frankie.expressionmanager.model.NavigationIndex
import com.frankie.expressionmanager.model.SurveyMode
import com.frankie.expressionmanager.usecase.SurveyDesignWithErrorException
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
        return processedSurvey.validationJsonOutput.script + "\n\n" + ScriptUtils().commonScript
    }
}