package com.frankie.backend.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Configuration
@Profile("local", "docker")
@Component
data class AwsCredentials(
    @Value("\${aws.accessId}")
    val accessId: String,
    @Value("\${aws.secretKey}")
    val secretKey: String,
)
