package com.qlarr.backend.mappers

import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.api.version.VersionDto
import com.qlarr.backend.persistence.entities.VersionEntity
import org.springframework.stereotype.Component

@Component
class VersionMapper {

    fun toDto(versionEntity: VersionEntity, surveyStatus: Status): VersionDto = VersionDto(
        surveyId = versionEntity.surveyId,
        version = versionEntity.version,
        subVersion = versionEntity.subVersion,
        valid = versionEntity.valid,
        published = versionEntity.published,
        lastModified = versionEntity.lastModified,
        status = surveyStatus
    )
}