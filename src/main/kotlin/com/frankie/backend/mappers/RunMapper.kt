package com.frankie.backend.mappers

import com.frankie.backend.api.runsurvey.RunSurveyDto
import com.frankie.backend.persistence.entities.SurveyEntity
import com.frankie.expressionmanager.model.SurveyLang
import com.frankie.expressionmanager.usecase.NavigationJsonOutput
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
        additionalLang,
        false
    )
}