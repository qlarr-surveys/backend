package com.qlarr.backend.services

import com.qlarr.backend.api.support.ContactRequest
import com.qlarr.backend.exceptions.InvalidInputException
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SupportServiceTest {

    private val emailService: EmailService = mockk()
    private val supportEmail = "support@test.com"

    private lateinit var supportService: SupportService

    @BeforeEach
    fun setup() {
        supportService = SupportService(emailService, supportEmail)
    }

    @Test
    fun `valid form sends email`() {
        every { emailService.sendEmail(any(), any(), any()) } just Runs

        supportService.submitContactForm(
            ContactRequest(name = "John", email = "john@example.com", message = "Hello")
        )

        verify(exactly = 1) {
            emailService.sendEmail(
                to = supportEmail,
                subject = any(),
                body = any()
            )
        }
    }

    @Test
    fun `blank name throws InvalidInputException`() {
        assertThrows<InvalidInputException> {
            supportService.submitContactForm(
                ContactRequest(name = "", email = "john@example.com", message = "Hello")
            )
        }
    }

    @Test
    fun `blank email throws InvalidInputException`() {
        assertThrows<InvalidInputException> {
            supportService.submitContactForm(
                ContactRequest(name = "John", email = "", message = "Hello")
            )
        }
    }

    @Test
    fun `invalid email throws InvalidInputException`() {
        assertThrows<InvalidInputException> {
            supportService.submitContactForm(
                ContactRequest(name = "John", email = "not-an-email", message = "Hello")
            )
        }
    }

    @Test
    fun `blank message throws InvalidInputException`() {
        assertThrows<InvalidInputException> {
            supportService.submitContactForm(
                ContactRequest(name = "John", email = "john@example.com", message = "")
            )
        }
    }
}
