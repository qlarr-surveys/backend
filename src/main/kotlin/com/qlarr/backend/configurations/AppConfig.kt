package com.qlarr.backend.configurations

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.qlarr.expressionmanager.model.jacksonKtMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {

    @Bean
    fun jacksonMapper(): ObjectMapper {
        jacksonKtMapper.registerModules(JavaTimeModule())
        jacksonKtMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return jacksonKtMapper
    }

    @Bean
    fun restTemplate():RestTemplate {
        return RestTemplate();
    }

}