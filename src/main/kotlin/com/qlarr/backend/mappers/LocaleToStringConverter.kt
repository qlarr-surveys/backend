package com.qlarr.backend.mappers

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.qlarr.surveyengine.model.*
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
