package com.qlarr.backend.configurations

import com.qlarr.backend.properties.EmailProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class EmailConfiguration(private val emailProperties: EmailProperties) {

    @Bean
    fun getMailSender(): JavaMailSender {
        val sender = JavaMailSenderImpl()
        sender.host = emailProperties.host
        sender.username = emailProperties.username
        sender.password = emailProperties.password
        sender.port = emailProperties.port
        val props = sender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        if (emailProperties.password.isNotEmpty()) {
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = emailProperties.starttls
            props["mail.smtp.ssl.enable"] = emailProperties.ssl
        }
        return sender

    }
}
