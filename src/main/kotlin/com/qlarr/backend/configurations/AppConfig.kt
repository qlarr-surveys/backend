package com.qlarr.backend.configurations

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.qlarr.backend.expressionmanager.*
import com.qlarr.surveyengine.model.ReservedCode
import com.qlarr.surveyengine.model.SurveyLang
import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import com.qlarr.surveyengine.model.exposed.ReturnType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return objectMapper
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate();
    }

}

val objectMapper:ObjectMapper = ObjectMapper()
    .registerModule(
        KotlinModule
            .Builder()
            .enable(KotlinFeature.NullIsSameAsDefault)
            .enable(KotlinFeature.NullToEmptyCollection)
            .enable(KotlinFeature.NullToEmptyMap)
            .build()
    ).registerModule(SimpleModule()
        .addSerializer(NavigationIndexSerializer())
        .addSerializer(NavigationDirectionSerializer())
        .addSerializer(SurveyLangSerializer())
        .addSerializer(ReturnTypeSerializer())
        .addSerializer(ReservedCodeSerializer())
        .addDeserializer(SurveyLang::class.java, SurveyLangDeserializer())
        .addDeserializer(NavigationIndex::class.java, NavigationIndexDeserializer())
        .addDeserializer(NavigationDirection::class.java, NavigationDirectionDeserializer())
        .addDeserializer(ReturnType::class.java, ReturnTypeDeserializer())
        .addDeserializer(SurveyLang::class.java, SurveyLangDeserializer())
        .addDeserializer(ReservedCode::class.java, ReservedCodeDeserializer())
    )
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .registerModules(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)