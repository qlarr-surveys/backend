package com.frankie.backend.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "fileSystem")
data class FileSystemProperties(
    val rootFolder: String
)
