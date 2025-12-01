package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.persistence.entities.AutoCompleteEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface AutoCompleteRepository : JpaRepository<AutoCompleteEntity, UUID> {

    fun findBySurveyIdAndComponentId(surveyId: UUID, componentId: String): AutoCompleteEntity?

    @Query(
        value = """
        SELECT DISTINCT
            elem.value #>> '{}' as match_value
        FROM 
            auto_complete ac
            CROSS JOIN LATERAL jsonb_array_elements(ac.data) AS elem(value)
        WHERE 
            ac.id = :uuid
            AND elem.value #>> '{}' ILIKE :searchTerm || '%'
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
    ): List<String>
}