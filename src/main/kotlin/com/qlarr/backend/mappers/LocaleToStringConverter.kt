package com.qlarr.backend.mappers

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.surveyengine.model.*
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import com.qlarr.surveyengine.model.exposed.ResponseField
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter


@Converter
class SchemaConverter :
    AttributeConverter<List<ResponseField>, String> {
    override fun convertToDatabaseColumn(attribute: List<ResponseField>): String {
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): List<ResponseField> {
        return objectMapper.readValue(dbData, jacksonTypeRef<List<ResponseField>>())
    }

}

@Converter
class NavigationIndexConverter :
    AttributeConverter<NavigationIndex, String> {
    override fun convertToDatabaseColumn(attribute: NavigationIndex): String {
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): NavigationIndex {
        return objectMapper.readValue(dbData, NavigationIndex::class.java)
    }

}
