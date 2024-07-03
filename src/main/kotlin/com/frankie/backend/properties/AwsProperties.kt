package com.frankie.backend.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
data class AwsProperties(
    @Value("\${aws.s3.bucketName}")
    val bucketName: String,
)
