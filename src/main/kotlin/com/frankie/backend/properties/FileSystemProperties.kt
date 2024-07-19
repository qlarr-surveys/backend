package com.frankie.backend.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
data class FileSystemProperties(
        @Value("\${fileSystem.rootFolder}")
    val rootFolder: String
)
