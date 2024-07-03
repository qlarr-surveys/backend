package com.frankie.expressionmanager.usecase

import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.model.ComponentIndex

data class DesignerInput(
    val state: ObjectNode,
    val componentIndexList: List<ComponentIndex>
)