package com.frankie.backend.end2end

import com.frankie.backend.api.user.*
import com.frankie.backend.exceptions.WrongResetTokenException
import com.frankie.backend.multitenancy.util.TenantContext
import com.frankie.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.frankie.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.util.*

class UserE2ETest : E2ETestBase() {


    @Test
    fun signUp_emptyDatabase_expect_newTenant() {
        val email = "email" + UUID.randomUUID() + "@koko.com"
        val createRequest = SignupRequest(
            firstName = "firstName",
            lastName = "lastName",
            email = email,
            password = "password"
        )

        webTestClient.post()
            .uri("/user/signup")
            .bodyValue(createRequest)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNoContent

        val nullTenantId = globalUserService.getTenantIdByEmail(email)
        assertThat(nullTenantId).isNull()

        val token = tenantRegistrationRepository.findByEmailAndConfirmedIsFalse(email).id

        webTestClient.post()
            .uri("/user/confirm_admin")
            .bodyValue(TokenRequest(token!!))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk

        val tenantId = globalUserService.getTenantIdByEmail(email)
        assertThat(tenantId).isNotNull
    }


    @Test
    fun create_adminCreatesUser_expect_user() {
        val adminEmail = "email" + UUID.randomUUID() + "@koko.com"
        val adminPassword = "admin_password"
        val domain = UUID.randomUUID().toString().lowercase().take(12)
        createTenant(adminEmail, adminPassword, domain)
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
        val userReg = userRegistrationRepository.findByEmail(email = userEmail)
        webTestClient.post()
                .uri("/user/confirm_new_user")
                .bodyValue(ConfirmUserRequest(token = userReg[0].id!!, newPassword = "fjhgfjhgf"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody(LoggedInUserResponse::class.java)
                .consumeWith(System.out::println)
                .returnResult()
        
        TenantContext.setTenantId(userReg[0].tenantId)
        val userEntity = userRepository.findByEmailAndDeletedIsFalse(userEmail)

        assertThat(userEntity!!.email).isEqualTo(userEmail)
        println("user created in database: $userEntity")
    }

}
