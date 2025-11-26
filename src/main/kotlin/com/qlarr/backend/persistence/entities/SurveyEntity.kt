package com.qlarr.backend.persistence.entities

import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.api.survey.Usage
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.mappers.SurveyNavigationDataConverter
import com.qlarr.surveyengine.model.exposed.NavigationMode
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

    @Column(name = "navigation_data")
    @Convert(converter = SurveyNavigationDataConverter::class)
    val navigationData: SurveyNavigationData,

    val image: String?,

    val description: String?,

    @Column(name = "response_review_required")
    val responseReviewRequired: Boolean,
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

data class SurveyNavigationData(
    val navigationMode: NavigationMode = NavigationMode.GROUP_BY_GROUP,
    val allowPrevious: Boolean = true,
    val resumeExpiryMillis: Long = TEN_YEARS_MILLIS,
    val skipInvalid: Boolean = true,
    val allowIncomplete: Boolean = true,
    val allowJump: Boolean = true
)

interface ResponseSummaryInterface {
    val id: UUID
    val index: Long
    val surveyId: UUID
    val surveyor: UUID?
    val values: Map<String, Any>
    val startDate: LocalDateTime
    val submitDate: LocalDateTime?
    val lang: String
    val preview: Boolean
    val disqualified: Boolean?
    val firstName: String?
    val lastName: String?


}

const val TEN_YEARS_MILLIS = 31536000000L