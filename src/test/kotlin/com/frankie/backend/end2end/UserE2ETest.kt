package com.frankie.backend.end2end

import com.frankie.backend.api.user.*
import com.frankie.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.frankie.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.util.*

class UserE2ETest : E2ETestBase() {




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

        val userEntity = userRepository.findByEmailAndDeletedIsFalse(userEmail)

        assertThat(userEntity!!.email).isEqualTo(userEmail)
        println("user created in database: $userEntity")
    }

}
