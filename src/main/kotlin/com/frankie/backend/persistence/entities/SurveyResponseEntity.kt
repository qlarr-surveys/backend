package com.frankie.backend.persistence.entities

import com.frankie.backend.mappers.NavigationIndexConverter
import com.frankie.expressionmanager.model.NavigationIndex
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UuidGenerator
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "responses")
data class SurveyResponseEntity(
        @Id
        @GeneratedValue
        @UuidGenerator
        val id: UUID? = null,

        @Column(name = "survey_id")
        val surveyId: UUID,

        val version: Int,

        val surveyor: UUID?,

        @Column(name = "nav_index")
        @Convert(converter = NavigationIndexConverter::class)
        val navigationIndex: NavigationIndex,

        @Column(name = "start_date")
        val startDate: LocalDateTime,

        @Column(name = "submit_date")
        val submitDate: LocalDateTime? = null,

        val lang: String,

        val preview: Boolean = false,

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "values", columnDefinition = "jsonb")
        val values: Map<String, Any> = mapOf()
)