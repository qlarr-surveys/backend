package com.frankie.backend.controllers

import com.frankie.backend.api.user.Roles
import com.frankie.backend.error.ControllerExceptionHandler
import com.frankie.backend.error.DefaultExceptionHandler
import com.frankie.backend.properties.JwtProperties
import com.frankie.backend.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@WebMvcTest
@Import(TestSecurityConfiguration::class)
@ContextConfiguration(classes = [ControllerExceptionHandler::class, DefaultExceptionHandler::class, JwtService::class, JwtProperties::class])
abstract class IntegrationTestBase {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var springSecurityFilterChain: jakarta.servlet.Filter

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    protected open fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .addFilters<DefaultMockMvcBuilder>(springSecurityFilterChain)
            .build()
    }

    open fun MockHttpServletRequestBuilder.withRoles(authorities: List<Roles> = emptyList()): MockHttpServletRequestBuilder {
        val mapped = authorities.map { SimpleGrantedAuthority(it.name.lowercase()) }
        val user = user("admin").password("pass").authorities(mapped)
        this.with(user)
        return this
    }

    open fun MockHttpServletRequestBuilder.withRole(role: Roles): MockHttpServletRequestBuilder {
        return withRoles(listOf(role))
    }

    companion object{

    }


}

@Configuration
@EnableMethodSecurity
class TestSecurityConfiguration
