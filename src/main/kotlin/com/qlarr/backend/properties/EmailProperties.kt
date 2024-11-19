package com.qlarr.backend.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class EmailProperties(
    @Value("\${mail.username}")
    val username: String,

    @Value("\${mail.host}")
    val host: String,

    @Value("\${mail.password}")
    val password: String,

    @Value("\${mail.port}")
    val port: Int,

    @Value("\${mail.ssl}")
    val ssl: String,

    @Value("\${mail.starttls}")
    val starttls: String

)