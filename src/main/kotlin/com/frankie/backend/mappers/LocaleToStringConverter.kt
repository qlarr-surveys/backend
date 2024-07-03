package com.frankie.backend.mappers

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.frankie.expressionmanager.model.*
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter


@Converter
class SchemaConverter :
    AttributeConverter<List<ResponseField>, String> {
    override fun convertToDatabaseColumn(attribute: List<ResponseField>): String {
        return jacksonKtMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): List<ResponseField> {
        return jacksonKtMapper.readValue(dbData, jacksonTypeRef<List<ResponseField>>())
    }

}

@Converter
class NavigationIndexConverter :
    AttributeConverter<NavigationIndex, String> {
    override fun convertToDatabaseColumn(attribute: NavigationIndex): String {
        return jacksonKtMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): NavigationIndex {
        return jacksonKtMapper.readValue(dbData, NavigationIndex::class.java)
    }

}

@Converter
class ResponseEventListConverter :
    AttributeConverter<List<ResponseEvent>, String> {
    private val mapper = jacksonKtMapper.registerModule(JavaTimeModule())
    override fun convertToDatabaseColumn(attribute: List<ResponseEvent>?): String {
        return mapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<ResponseEvent> {
        return mapper.readValue(dbData, jacksonTypeRef<List<ResponseEvent>>())
    }

}
