package com.frankie.backend.configurations

import com.frankie.backend.properties.EmailProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class EmailConfiguration(private val props: EmailProperties) {

    @Bean
    fun getMailSender(): JavaMailSender {
        val sender = JavaMailSenderImpl()
        sender.host = props.host
        sender.username = props.email
        sender.password = props.password
        sender.port = props.port
        val props = sender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.debug"] = "true"
        props["mail.smtp.ssl.enable"] = "true"
        return sender
    }
}
