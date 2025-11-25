package com.qlarr.backend.persistence.entities

import com.fasterxml.jackson.databind.node.ArrayNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*

@Entity
@Table(name = "auto_complete")
data class AutoCompleteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID?,

    @Column(name = "survey_id", nullable = false)
    val surveyId: UUID,

    @Column(name = "component_id", nullable = false)
    val componentId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    val values: ArrayNode
)