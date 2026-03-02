package com.qlarr.backend.services

import com.qlarr.backend.api.support.ContactRequest
import com.qlarr.backend.common.isValidEmail
import com.qlarr.backend.exceptions.InvalidInputException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SupportService(
    private val emailService: EmailService,
    @Value("\${support.email}") private val supportEmail: String
) {

    fun submitContactForm(contactRequest: ContactRequest) {
        if (contactRequest.name.isBlank()) {
            throw InvalidInputException("Name is required")
        }
        if (contactRequest.email.isBlank() || !contactRequest.email.isValidEmail()) {
            throw InvalidInputException("Valid email is required")
        }
        if (contactRequest.message.isBlank()) {
            throw InvalidInputException("Message is required")
        }

        val subject = "Contact Form: ${contactRequest.name}"
        val body = """
            <h3>New Contact Form Submission</h3>
            <p><strong>Name:</strong> ${contactRequest.name}</p>
            <p><strong>Email:</strong> ${contactRequest.email}</p>
            <p><strong>Message:</strong></p>
            <p>${contactRequest.message}</p>
        """.trimIndent()

        emailService.sendEmail(to = supportEmail, subject = subject, body = body)
    }
}
