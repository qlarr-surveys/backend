package com.frankie.backend.api.response

import com.frankie.expressionmanager.model.NavigationIndex
import java.time.LocalDateTime

data class UploadResponseRequestData(
        val versionId: Int,
        val lang: String,
        val values: Map<String, Any> = mapOf(),
        val startDate: LocalDateTime,
        val submitDate: LocalDateTime,
        val userId: String,
        val navigationIndex: NavigationIndex
)
