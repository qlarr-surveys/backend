package com.qlarr.backend.api.design

import com.qlarr.backend.api.version.VersionDto
import com.qlarr.surveyengine.usecase.DesignerInput

data class DesignDto(
    val designerInput: DesignerInput,
    val versionDto: VersionDto
)


