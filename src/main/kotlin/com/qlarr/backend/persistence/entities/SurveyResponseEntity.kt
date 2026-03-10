package com.qlarr.backend.persistence.entities

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.qlarr.backend.api.response.ResponseEvent
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.backend.mappers.NavigationIndexConverter
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "responses")
data class SurveyResponseEntity(
        @Id

        val id: UUID,

        @Column(name = "survey_id")
        val surveyId: UUID,

        val version: Int,

        val surveyor: UUID?,

        @Column(name = "survey_response_index", updatable = false, insertable = false)
        val surveyResponseIndex: Int? = null,

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
        val values: Map<String, Any> = mapOf(),

        // Enterprise
        @Column(name = "ip_addr")
        val ipAddress: String? = null,

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "events", columnDefinition = "jsonb")
        @Convert(converter = ResponseEventListConverter::class)
        val events: List<ResponseEvent> = listOf(),
)

@Converter
class ResponseEventListConverter :
        AttributeConverter<List<ResponseEvent>, String> {
        private val mapper = objectMapper.registerModule(JavaTimeModule())
        override fun convertToDatabaseColumn(attribute: List<ResponseEvent>): String {
                return mapper.writeValueAsString(attribute)
        }

        override fun convertToEntityAttribute(dbData: String): List<ResponseEvent> {
                return mapper.readValue(dbData, jacksonTypeRef<List<ResponseEvent>>())
        }

}