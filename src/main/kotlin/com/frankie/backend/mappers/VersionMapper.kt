package com.frankie.backend.mappers

import com.frankie.backend.api.survey.Status
import com.frankie.backend.api.version.VersionDto
import com.frankie.backend.persistence.entities.VersionEntity
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