package com.frankie.expressionmanager.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.json.JSONObject

val jacksonKtMapper: ObjectMapper = ObjectMapper()
    .registerModule(
        KotlinModule
            .Builder()
            .enable(KotlinFeature.NullIsSameAsDefault)
            .enable(KotlinFeature.NullToEmptyCollection)
            .enable(KotlinFeature.NullToEmptyMap)
            .build()
    )
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

fun JSONObject.toObjectNode(): ObjectNode = jacksonKtMapper.readTree(toString()) as ObjectNode