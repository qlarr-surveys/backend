package com.frankie.backend.end2end

import com.frankie.backend.api.user.LoggedInUserResponse
import com.frankie.backend.api.user.LoginRequest
import com.frankie.backend.api.user.SignupRequest
import com.frankie.backend.api.user.TokenRequest
import com.frankie.backend.multitenancy.configurations.tenant.hibernate.SchemaBasedMultiTenantConnectionProvider
import com.frankie.backend.multitenancy.repositories.TenantRegistrationRepository
import com.frankie.backend.multitenancy.repositories.UserRegistrationRepository
import com.frankie.backend.multitenancy.service.GlobalUserService
import com.frankie.backend.persistence.repositories.UserRepository
import com.frankie.backend.services.EmailService
import com.frankie.backend.services.SurveyService
import com.frankie.backend.services.UserService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * To run locally you must start docker desktop or any docker environment
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class E2ETestBase {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var globalUserService: GlobalUserService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userRegistrationRepository: UserRegistrationRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var tenantRegistrationRepository: TenantRegistrationRepository


    companion object {
        private val postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15.1")
            .withDatabaseName("frankie_test")
            .withUsername("frankie_user")
            .withPassword("frankie_pass")
            .withReuse(true)

        @DynamicPropertySource
        @JvmStatic
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgreSQLContainer::getUsername)
            registry.add("spring.datasource.password", postgreSQLContainer::getPassword)
        }

        @BeforeAll
        @JvmStatic
        fun startContainer() {
            if (!postgreSQLContainer.isRunning) {
                postgreSQLContainer.start()
            }
        }
    }


    fun createTenant(email: String, password: String, domain:String) {
        val createRequest = SignupRequest(
            firstName = "firstName",
            lastName = "lastName",
            email = email,
            password = password
        )

        webTestClient.post()
            .uri("/user/signup")
            .bodyValue(createRequest)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNoContent

        val nullTenantId = globalUserService.getTenantIdByEmail(email)
        Assertions.assertThat(nullTenantId).isNull()

        val token = tenantRegistrationRepository.findByEmailAndConfirmedIsFalse(email).id

        webTestClient.post()
            .uri("/user/confirm_admin")
            .bodyValue(TokenRequest(token!!))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk

        val tenantId = globalUserService.getTenantIdByEmail(email)
        Assertions.assertThat(tenantId).isNotNull
    }

    fun login(email: String, password: String): String {
        val loginRequest = LoginRequest(email, password)
        val loginResponseBody: LoggedInUserResponse = webTestClient.post()
            .uri("/user/login")
            .bodyValue(loginRequest)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectBody(LoggedInUserResponse::class.java)
            .consumeWith(System.out::println)
            .returnResult()
            .responseBody as LoggedInUserResponse

        return loginResponseBody.accessToken
    }

}
