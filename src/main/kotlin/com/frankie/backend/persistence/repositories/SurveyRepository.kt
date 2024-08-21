package com.frankie.backend.persistence.repositories

import com.frankie.backend.api.survey.Status
import com.frankie.backend.persistence.entities.OfflineSurveyResponseCount
import com.frankie.backend.persistence.entities.SurveyEntity
import com.frankie.backend.persistence.entities.SurveyResponseCount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface SurveyRepository : CrudRepository<SurveyEntity, UUID> {

    @Query(
            "SELECT s as survey, COUNT(r) as responseCount, " +
                    "v as latestVersion, " +
                    "COUNT(case when r.submitDate IS NOT NULL then 1 else null end) as completeResponseCount " +
                    "FROM SurveyEntity s " +
                    "LEFT JOIN SurveyResponseEntity r ON s.id = r.surveyId AND r.preview = false " +
                    "JOIN VersionEntity v ON v.surveyId = s.id AND v.version = (SELECT MAX(p.version) " +
                    "FROM VersionEntity p WHERE s.id = p.surveyId) " +
                    "WHERE (:status is null OR s.status = :status) " +
                    "AND (:scheduled = false OR s.startDate > NOW()) " +
                    "AND (:expired = false OR s.endDate < NOW()) " +
                    "AND (:active = false OR (s.startDate is null OR s.startDate < NOW()) AND (s.endDate is null OR s.endDate > NOW())) " +
                    "GROUP BY s.id, v.surveyId, v.version " +
                    "ORDER BY s.lastModified DESC, completeResponseCount DESC "
    )
    fun findAllSurveysSortByLastModified(active: Boolean,
                                         scheduled: Boolean,
                                         expired: Boolean,
                                         status: Status?,
                                         pageable: Pageable)
            : Page<SurveyResponseCount>

    @Query(
            "SELECT s as survey, COUNT(r) as responseCount, " +
                    "v as latestVersion, " +
                    "COUNT(case when r.submitDate IS NOT NULL then 1 else null end) as completeResponseCount " +
                    "FROM SurveyEntity s " +
                    "LEFT JOIN SurveyResponseEntity r ON s.id = r.surveyId AND r.preview = false " +
                    "JOIN VersionEntity v ON v.surveyId = s.id AND v.version = (SELECT MAX(p.version) " +
                    "FROM VersionEntity p WHERE s.id = p.surveyId) " +
                    "WHERE (:status is null OR s.status = :status) " +
                    "AND (:scheduled = false OR s.startDate > NOW()) " +
                    "AND (:expired = false OR s.endDate < NOW()) " +
                    "AND (:active = false OR (s.startDate is null OR s.startDate < NOW()) AND (s.endDate is null OR s.endDate > NOW())) " +
                    "GROUP BY s.id, v.surveyId, v.version " +
                    "ORDER BY completeResponseCount DESC, s.lastModified DESC "
    )
    fun findAllSurveysSortByResponses(active: Boolean,
                                      scheduled: Boolean,
                                      expired: Boolean,
                                      status: Status?,
                                      pageable: Pageable)
            : Page<SurveyResponseCount>


    @Query(
            "SELECT s as survey, v as latestVersion, " +
                    "COUNT(r.submitDate) as completeResponseCount, " +
                    "COUNT(case when r.surveyor = :userId then 1 else null end) as userResponseCount " +
                    "FROM SurveyEntity s " +
                    "JOIN VersionEntity v ON v.surveyId = s.id AND v.version = (SELECT MAX(p.version) FROM VersionEntity p WHERE p.surveyId = s.id AND p.published = TRUE GROUP BY p.surveyId) " +
                    "LEFT JOIN SurveyResponseEntity r ON s.id = r.surveyId AND r.preview = false AND r.submitDate IS NOT NULL " +
                    "WHERE (s.status = 'ACTIVE' AND (s.usage = 'OFFLINE' OR s.usage = 'MIXED')) " +
                    "GROUP BY s.id, v.surveyId, v.version "
    )
    fun findAllOfflineSurveysByUserId(userId: UUID): List<OfflineSurveyResponseCount>
}

enum class SurveySort {
    RESPONSES_DESC,
    LAST_MODIFIED_DESC;

    companion object {
        fun parse(input: String?): SurveySort = values().firstOrNull {
            it.name.lowercase() == input
        } ?: LAST_MODIFIED_DESC
    }
}

enum class SurveyFilter(val status: Status?) {
    ALL(null),
    DRAFT(Status.DRAFT),
    ACTIVE(Status.ACTIVE),
    SCHEDULED(Status.ACTIVE),
    EXPIRED(Status.ACTIVE),
    CLOSED(Status.CLOSED);

    companion object {
        fun parse(input: String?): SurveyFilter = SurveyFilter.values().firstOrNull {
            it.name.lowercase() == input
        } ?: ALL
    }
}
