package com.frankie.backend.end2end

import com.frankie.backend.api.user.*
import com.frankie.backend.persistence.repositories.UserRepository
import com.frankie.backend.security.JwtService
import com.frankie.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.frankie.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.util.*

class UserE2ETest : E2ETestBase() {

    @Autowired
    lateinit var jwtService: JwtService


    @Test
    fun create_adminCreatesUser_expect_user() {
        val adminEmail = "admin@admin.admin"
        val adminPassword = "admin"
        val authToken = login(adminEmail, adminPassword)

        val userEmail = "email" + UUID.randomUUID() + "@koko.com"
        val normalUserCreateRequest = CreateRequest(
            firstName = "user_firstName",
            lastName = "user_lastName",
            email = userEmail,
            roles = setOf(Roles.SURVEYOR)
        )

        webTestClient.post()
            .uri("/user/create")
            .header(HEADER_STRING, TOKEN_PREFIX + authToken)
            .bodyValue(normalUserCreateRequest)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDTO::class.java)
            .consumeWith(System.out::println)
            .returnResult()
        val userEntityUnConfirmed = userRepository.findByEmailAndDeletedIsFalse(userEmail)
        val token = jwtService.generatePasswordResetToken(userEntityUnConfirmed!!,true)
        webTestClient.post()
                .uri("/user/reset_password")
                .bodyValue(ResetPasswordRequest(refreshToken = token, newPassword = "fjhgfjhgf"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody(LoggedInUserResponse::class.java)
                .consumeWith(System.out::println)
                .returnResult()

        val userEntity = userRepository.findByEmailAndDeletedIsFalse(userEmail)

        assertThat(userEntity!!.email).isEqualTo(userEmail)
        assertThat(userEntity.isConfirmed).isEqualTo(true)
        println("user created in database: $userEntity")
    }

}
