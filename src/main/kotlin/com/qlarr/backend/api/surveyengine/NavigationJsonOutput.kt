package com.qlarr.backend.api.surveyengine

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.surveyengine.model.exposed.NavigationIndex


data class NavigationJsonOutput(
    val survey: ObjectNode = JsonNodeFactory.instance.objectNode(),
    val state: ObjectNode = JsonNodeFactory.instance.objectNode(),
    val navigationIndex: NavigationIndex,
    val toSave: Map<String, Any> = mapOf()
)