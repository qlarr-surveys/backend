package com.frankie.backend.api.design

import com.frankie.backend.api.version.VersionDto
import com.frankie.expressionmanager.usecase.DesignerInput

data class DesignDto(
    val designerInput: DesignerInput,
    val versionDto: VersionDto
)


