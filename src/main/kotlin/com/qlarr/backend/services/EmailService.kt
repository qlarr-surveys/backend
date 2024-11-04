package com.qlarr.backend.services

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

/**
 * This service send only emails.
 * For more examples see here: https://mailtrap.io/blog/spring-send-email/
 */
@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
) {

    fun sendEmail(to: String, subject: String, body: String) {
        val message = javaMailSender.createMimeMessage();
        val helper = MimeMessageHelper(message, true)
        helper.setTo(to)
        helper.setSubject(subject);
        helper.setText(body, true)
        javaMailSender.send(message)
    }
}