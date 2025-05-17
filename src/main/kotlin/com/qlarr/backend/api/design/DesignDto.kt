package com.qlarr.backend.api.design

import com.qlarr.backend.api.surveyengine.DesignerInput
import com.qlarr.backend.api.version.VersionDto

data class DesignDto(
    val designerInput: DesignerInput,
    val versionDto: VersionDto
)


