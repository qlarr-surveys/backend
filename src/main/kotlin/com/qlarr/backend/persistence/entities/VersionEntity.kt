package com.qlarr.backend.persistence.entities

import com.qlarr.backend.mappers.SchemaConverter
import com.qlarr.surveyengine.model.ResponseField
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*


@Entity
@Table(name = "versions")
@IdClass(VersionId::class)
data class VersionEntity(
    @Id
    val version: Int,
    @Id
    @Column(name = "survey_id")
    val surveyId: UUID,
    @Column(name = "sub_version")
    val subVersion: Int,
    val valid: Boolean,
    val published: Boolean,
    @Convert(converter = SchemaConverter::class)
    val schema: List<ResponseField>,
    @Column(name = "last_modified")
    val lastModified: LocalDateTime?
)

data class VersionId(
    private val version: Int = 1,
    private val surveyId: UUID = UUID.randomUUID()
)