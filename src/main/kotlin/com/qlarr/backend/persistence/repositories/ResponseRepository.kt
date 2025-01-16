package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.persistence.entities.ResponseCount
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
            "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                    "FROM SurveyResponseEntity r " +
                    "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                    "WHERE r.surveyId = :surveyId AND r.surveyor = :surveyor " +
                    "ORDER BY r.startDate ASC"
    )
    fun findAllBySurveyIdAndSurveyor(surveyId: UUID, surveyor: UUID, pageable: Pageable): Page<ResponseWithSurveyorName>

    @Query(
            "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                    "FROM SurveyResponseEntity r " +
                    "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                    "WHERE r.surveyId = :surveyId AND r.preview = false " +
                    "ORDER BY r.startDate ASC"
    )
    fun findAllBySurveyId(surveyId: UUID, pageable: Pageable): Page<ResponseWithSurveyorName>

    @Query(
            "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                    "FROM SurveyResponseEntity r " +
                    "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                    "WHERE r.surveyId = :surveyId AND r.submitDate IS NOT NULL AND r.preview = false " +
                    "ORDER BY r.startDate ASC"
    )
    fun findAllBySurveyIdAndSubmitDateIsNotNull(
            surveyId: UUID,
            pageable: Pageable
    ): Page<ResponseWithSurveyorName>

    @Query(
            "SELECT r as response, u.firstName as firstName, u.lastName as lastName " +
                    "FROM SurveyResponseEntity r " +
                    "LEFT JOIN UserEntity u ON r.surveyor = u.id " +
                    "WHERE r.surveyId = :surveyId AND r.submitDate IS NULL AND r.preview = false"
    )
    fun findAllBySurveyIdAndSubmitDateIsNull(
            surveyId: UUID,
            pageable: Pageable
    ): Page<ResponseWithSurveyorName>

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

}
