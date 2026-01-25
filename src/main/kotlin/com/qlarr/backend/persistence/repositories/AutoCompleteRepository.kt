package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.persistence.entities.AutoCompleteEntity
import com.qlarr.backend.persistence.entities.AutoCompleteId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface AutoCompleteRepository : JpaRepository<AutoCompleteEntity, AutoCompleteId> {

    fun findBySurveyId(surveyId: UUID): List<AutoCompleteEntity>

    fun findBySurveyIdAndComponentId(surveyId: UUID, componentId: String): AutoCompleteEntity?

    @Query(
        value = """
        SELECT DISTINCT
            elem.value #>> '{}' as match_value
        FROM
            auto_complete ac
            CROSS JOIN LATERAL jsonb_array_elements(ac.data) AS elem(value)
        WHERE
            ac.survey_id = :surveyId
            AND ac.filename = :filename
            AND elem.value #>> '{}' ILIKE :searchTerm || '%'
        ORDER BY
            match_value
        LIMIT :limit
    """,
        nativeQuery = true
    )
    fun searchAutoComplete(
        surveyId: UUID,
        filename: String,
        searchTerm: String,
        limit: Int = 10
    ): List<String>

    @Modifying
    @Query(
        value = """
            INSERT INTO auto_complete (survey_id, component_id, filename, data)
            SELECT
                :destinationSurveyId,
                component_id,
                filename,
                data
            FROM auto_complete
            WHERE survey_id = :sourceSurveyId
        """,
        nativeQuery = true
    )
    fun copyAutoCompleteEntries(sourceSurveyId: UUID, destinationSurveyId: UUID): Int
}
