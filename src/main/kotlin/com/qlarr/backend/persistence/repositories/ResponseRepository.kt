package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.persistence.entities.ResponseCount
import com.qlarr.backend.persistence.entities.ResponseSummaryInterface
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.services.ResponseWithSurveyorName
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ResponseRepository : JpaRepository<SurveyResponseEntity, UUID> {


    @Query(
        "SELECT COUNT(r.submitDate) as completeResponseCount, " +
                "COUNT(case when r.surveyor = :userId then 1 else null end) as userResponseCount " +
                "FROM SurveyResponseEntity r " +
                "WHERE  r.surveyId = :surveyId AND r.preview = false AND r.submitDate IS NOT NULL"
    )
    fun responseCount(userId: UUID, surveyId: UUID): ResponseCount


    @Query(
        "SELECT COUNT(*)" +
                " FROM responses" +
                " WHERE survey_id = :surveyId and submit_date IS NOT NULL AND preview = false", nativeQuery = true
    )
    fun completedSurveyCount(surveyId: UUID): Int

    fun deleteBySurveyId(surveyId: UUID)

    @Transactional
    @Modifying
    @Query("UPDATE responses SET version = :to WHERE survey_id = :surveyId AND version = :from", nativeQuery = true)
    fun changeVersion(surveyId: UUID, from: Int, to: Int)


    @Query(
        "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                "FROM SurveyResponseEntity r " +
                "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                "WHERE r.surveyId = :surveyId AND r.preview = false " +
                "AND r.surveyResponseIndex >= :from AND r.surveyResponseIndex <= :to " +
                "ORDER BY r.surveyResponseIndex ASC"
    )
    fun fromToBySurveyId(surveyId: UUID, from: Int, to: Int): List<ResponseWithSurveyorName>


    @Query(
        "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                "FROM SurveyResponseEntity r " +
                "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                "WHERE r.surveyId = :surveyId AND r.submitDate IS NOT NULL AND r.preview = false " +
                "AND r.surveyResponseIndex >= :from AND r.surveyResponseIndex <= :to " +
                "ORDER BY r.surveyResponseIndex ASC"
    )
    fun fromToBySurveyIdAndSubmitDateIsNotNull(
        surveyId: UUID,
        from: Int,
        to: Int
    ): List<ResponseWithSurveyorName>


    @Query(
        "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                "FROM SurveyResponseEntity r " +
                "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                "WHERE r.surveyId = :surveyId AND r.submitDate IS NULL AND r.preview = false " +
                "AND r.surveyResponseIndex >= :from AND r.surveyResponseIndex <= :to " +
                "ORDER BY r.surveyResponseIndex ASC"
    )
    fun fromToBySurveyIdAndSubmitDateIsNull(
        surveyId: UUID,
        from: Int,
        to: Int
    ): List<ResponseWithSurveyorName>

    @Query(
        "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                "FROM SurveyResponseEntity r " +
                "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                "WHERE r.id = :responseId"
    )
    fun responseWithSurveyorName(
        responseId:UUID,
    ): ResponseWithSurveyorName?


    @Query(
        "SELECT r.id as id, r.survey_response_index as index, r.survey_id as surveyId, r.surveyor as surveyor, r.start_date as startDate, " +
                "r.submit_date as submitDate,r.values as values, r.preview as preview, r.lang as lang, " +
                "CAST((r.values  ->> 'Survey.disqualified') AS boolean) as disqualified, u.first_name as firstName, u.last_name as lastName " +
                "FROM responses r " +
                "LEFT JOIN users u ON r.surveyor = u.id " +
                "WHERE r.survey_id = :surveyId AND r.surveyor = :surveyor " +
                "ORDER BY r.start_date ASC",
        nativeQuery = true
    )
    fun summaryBySurveyor(surveyId: UUID, surveyor: UUID, pageable: Pageable): Page<ResponseSummaryInterface>

    @Query(
        "SELECT r.id as id, r.survey_response_index as index, r.survey_id as surveyId, r.surveyor as surveyor, r.start_date as startDate, " +
                "r.submit_date as submitDate,r.values as values, r.preview as preview, r.lang as lang, " +
                "CAST((r.values  ->> 'Survey.disqualified') AS boolean) as disqualified, u.first_name as firstName, u.last_name as lastName " +
                "FROM responses r " +
                "LEFT JOIN users u ON r.surveyor = u.id " +
                "WHERE r.survey_id = :surveyId AND r.preview = TRUE " +
                "ORDER BY r.start_date ASC",
        nativeQuery = true
    )
    fun previewSummary(surveyId: UUID, pageable: Pageable): Page<ResponseSummaryInterface>

    @Query(
        "SELECT r.id as id, r.survey_response_index as index, r.survey_id as surveyId, r.surveyor as surveyor, r.start_date as startDate, " +
                "r.submit_date as submitDate,r.values as values, r.preview as preview, r.lang as lang, " +
                "CAST((r.values  ->> 'Survey.disqualified') AS boolean) as disqualified, u.first_name as firstName, u.last_name as lastName " +
                "FROM responses r " +
                "LEFT JOIN users u ON r.surveyor = u.id " +
                "WHERE r.survey_id = :surveyId AND r.preview = FALSE AND r.submit_date IS NOT NULL  " +
                "ORDER BY r.start_date ASC",
        nativeQuery = true
    )
    fun completeSummary(surveyId: UUID, pageable: Pageable): Page<ResponseSummaryInterface>

    @Query(
        "SELECT r.id as id, r.survey_response_index as index, r.survey_id as surveyId, r.surveyor as surveyor, r.start_date as startDate, " +
                "r.submit_date as submitDate,r.values as values, r.preview as preview, r.lang as lang, " +
                "CAST((r.values  ->> 'Survey.disqualified') AS boolean) as disqualified, u.first_name as firstName, u.last_name as lastName " +
                "FROM responses r " +
                "LEFT JOIN users u ON r.surveyor = u.id " +
                "WHERE r.survey_id = :surveyId AND r.preview = FALSE AND r.submit_date IS NULL  " +
                "ORDER BY r.start_date ASC",
        nativeQuery = true
    )
    fun incompleteSummary(surveyId: UUID, pageable: Pageable): Page<ResponseSummaryInterface>


    @Query(
        "SELECT r.id as id, r.survey_response_index as index, r.survey_id as surveyId, r.surveyor as surveyor, r.start_date as startDate, " +
                "r.submit_date as submitDate,r.values as values, r.preview as preview, r.lang as lang, " +
                "CAST((r.values  ->> 'Survey.disqualified') AS boolean) as disqualified, u.first_name as firstName, u.last_name as lastName " +
                "FROM responses r " +
                "LEFT JOIN users u ON r.surveyor = u.id " +
                "WHERE r.survey_id = :surveyId " +
                "ORDER BY r.start_date ASC",
        nativeQuery = true
    )
    fun summary(surveyId: UUID, pageable: Pageable): Page<ResponseSummaryInterface>


}
