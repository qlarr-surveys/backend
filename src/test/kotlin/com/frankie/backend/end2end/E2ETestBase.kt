package com.frankie.backend.end2end

import com.frankie.backend.api.user.LoggedInUserResponse
import com.frankie.backend.api.user.LoginRequest
import com.frankie.backend.persistence.repositories.UserRegistrationRepository
import com.frankie.backend.persistence.repositories.UserRepository
import com.frankie.backend.services.UserService
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
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userRegistrationRepository: UserRegistrationRepository

    @Autowired
    lateinit var userService: UserService


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
