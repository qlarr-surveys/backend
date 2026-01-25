package com.qlarr.backend.persistence.entities

import com.fasterxml.jackson.databind.node.ArrayNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.util.*

data class AutoCompleteId(
    val surveyId: UUID = UUID.randomUUID(),
    val filename: String = ""
) : Serializable

@Entity
@Table(
    name = "auto_complete",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["survey_id", "component_id"])
    ]
)
@IdClass(AutoCompleteId::class)
data class AutoCompleteEntity(
    @Id
    @Column(name = "survey_id", nullable = false)
    val surveyId: UUID,

    @Id
    @Column(name = "filename", nullable = false)
    val filename: String,

    @Column(name = "component_id", nullable = false)
    val componentId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    val values: ArrayNode
)