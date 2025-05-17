package com.qlarr.backend.mappers

import com.qlarr.backend.api.runsurvey.RunSurveyDto
import com.qlarr.backend.api.surveyengine.NavigationJsonOutput
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.surveyengine.model.SurveyLang
import org.springframework.stereotype.Component
import java.util.*

@Component
class RunMapper {

    fun toRunDto(
        responseId: UUID,
        lang: SurveyLang,
        additionalLang: List<SurveyLang>,
        navigationJsonOutput: NavigationJsonOutput,
        surveyEntity: SurveyEntity
    ) = RunSurveyDto(
        survey = navigationJsonOutput.survey,
        state = navigationJsonOutput.state,
        navigationIndex = navigationJsonOutput.navigationIndex,
        responseId,
        lang,
        additionalLang)
}