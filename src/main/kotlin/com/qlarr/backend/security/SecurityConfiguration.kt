package com.qlarr.backend.security

import com.qlarr.backend.api.user.Roles.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(
    private val authenticationFilter: JwtTokenFilter,
    private val logoutHandler: LogoutServiceHandler
) {

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
        http.cors(Customizer.withDefaults())
        http.headers().frameOptions().sameOrigin()
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        http.authorizeHttpRequests { authorize ->
            authorize
                .requestMatchers(OPTIONS).permitAll()
//                    .requestMatchers(GET, "/swagger-ui/**").permitAll()
//                    .requestMatchers(GET, "/v3/api-docs").permitAll()
//                    .requestMatchers(GET, "/v3/api-docs/**").permitAll()

                .requestMatchers(GET, "/survey/{surveyId}/autocomplete/{uuid}").permitAll()
                .requestMatchers(POST, "/survey/{surveyId}/run/start").permitAll()
                .requestMatchers(POST, "/survey/{surveyId}/run/navigate").permitAll()
                .requestMatchers(GET, "/survey/{surveyId}/run/runtime.js").permitAll()
                .requestMatchers(GET, "/survey/{surveyId}/response/{responseId}/attach/{filename}").permitAll()
                .requestMatchers(GET, "/survey/{surveyId}/response/attach/{responseId}/{questionId}").permitAll()
                .requestMatchers(POST, "/survey/{surveyId}/response/attach/{responseId}/{questionId}").permitAll()
                .requestMatchers(GET, "/survey/{surveyId}/resource/{fileName}").permitAll()
                .requestMatchers(POST, "/user/login").permitAll()
                .requestMatchers(POST, "/user/forgot_password").permitAll()
                .requestMatchers(POST, "/user/reset_password").permitAll()
                .requestMatchers(POST, "/user/refresh_token").permitAll()
        }
        http.authorizeHttpRequests()
            .anyRequest()
            .authenticated()
            .and()
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        http.httpBasic(Customizer.withDefaults())
        http.logout().logoutUrl("/logout").addLogoutHandler(logoutHandler)
            .logoutSuccessHandler { _, _, _ -> SecurityContextHolder.clearContext() }
            .permitAll()
        return http.build()
    }

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager? {
        return config.authenticationManager
    }

}


