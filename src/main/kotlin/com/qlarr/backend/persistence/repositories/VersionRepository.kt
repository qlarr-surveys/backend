package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.entities.VersionId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface VersionRepository : CrudRepository<VersionEntity, VersionId> {
    @Query(
        "SELECT a.*\n" +
                "FROM versions a\n" +
                "INNER JOIN (\n" +
                "    SELECT survey_id, MAX(version) as version\n" +
                "    FROM versions\n" +
                "    WHERE survey_id = :surveyId AND published = true\n" +
                "    GROUP BY survey_id\n" +
                ") b ON a.survey_id = b.survey_id AND a.version = b.version\n", nativeQuery = true
    )
    fun findLatestPublishedVersion(surveyId: UUID): VersionEntity?

    @Query(
        "SELECT a.*\n" +
                "FROM versions a\n" +
                "INNER JOIN (\n" +
                "    SELECT survey_id, MAX(version) as version\n" +
                "    FROM versions c\n" +
                "    WHERE survey_id = :surveyId\n" +
                "    GROUP BY survey_id\n" +
                ") b ON a.survey_id = b.survey_id AND a.version = b.version\n", nativeQuery = true
    )
    fun findLatestVersion(surveyId: UUID): VersionEntity?

    fun findBySurveyIdAndVersion(surveyId: UUID, version: Int): VersionEntity?

    fun deleteBySurveyId(surveyId: UUID)
}