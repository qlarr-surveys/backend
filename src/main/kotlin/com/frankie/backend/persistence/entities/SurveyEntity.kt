package com.frankie.backend.persistence.entities

import com.frankie.backend.api.survey.Status
import com.frankie.backend.api.survey.Usage
import com.frankie.backend.common.nowUtc
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "surveys")
data class SurveyEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "creation_date")
    val creationDate: LocalDateTime?,

    @Column(name = "last_modified")
    val lastModified: LocalDateTime?,

    @Column(unique = true, nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    val status: Status,

    @Column(name = "start_date")
    val startDate: LocalDateTime?,

    @Column(name = "end_date")
    val endDate: LocalDateTime?,

    @Enumerated(EnumType.STRING)
    val usage: Usage,

    val quota: Int,

    @Column(name = "can_lock_survey")
    val canLockSurvey: Boolean,

    val image: String?,

    val description: String?,
) {
    fun isActive(): Boolean {
        return status == Status.ACTIVE
                && (endDate == null || endDate.isAfter(nowUtc()))
                && (startDate == null || startDate.isBefore(nowUtc()))
    }
}

interface SurveyResponseCount {
    val survey: SurveyEntity
    val responseCount: Long
    val completeResponseCount: Long
    val latestVersion: VersionEntity
}

interface OfflineSurveyResponseCount {
    val survey: SurveyEntity
    val completeResponseCount: Long
    val latestVersion: VersionEntity
    val userResponseCount: Long
}

interface ResponseCount {
    val completeResponseCount: Long
    val userResponseCount: Long
}