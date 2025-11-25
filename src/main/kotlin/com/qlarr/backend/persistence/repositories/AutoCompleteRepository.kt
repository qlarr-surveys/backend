package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.persistence.entities.AutoCompleteEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface AutoCompleteRepository : JpaRepository<AutoCompleteEntity, UUID> {

    fun findBySurveyIdAndComponentId(surveyId: UUID, componentId: String): AutoCompleteEntity?

    @Query(
        value = """
            SELECT 
                elem.value as match_value
            FROM 
                auto_complete ac,
                jsonb_array_elements(ac.data) WITH ORDINALITY AS elem(value, idx)
            WHERE 
                ac.id = :uuid
                AND (
                    (jsonb_typeof(elem.value) = 'string' 
                     AND (elem.value #>> '{}') ILIKE CONCAT(:searchTerm, '%'))
                    OR
                    (jsonb_typeof(elem.value) = 'object' 
                     AND (elem.value ->> 'key') ILIKE CONCAT(:searchTerm, '%'))
                )
            ORDER BY 
                match_value
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchAutoComplete(
        uuid: UUID,
        searchTerm: String,
        limit: Int = 10
    ): List<Any>
}