package com.qlarr.surveyengine.model

import com.qlarr.surveyengine.ext.splitToComponentCodes
import kotlinx.serialization.Serializable

@Serializable
data class ComponentIndex(
    val code: String,
    val parent: String?,
    val children: List<String> = listOf(),
    val minIndex: Int,
    val maxIndex: Int,
    val prioritisedSiblings: Set<String> = setOf(),
    val dependencies: Set<ReservedCode> = setOf()
) {
    fun hasSkip() = dependencies.any { it is ReservedCode.Skip }
}

fun List<ComponentIndex>.parents(code: String): List<String> {
    return if (code == "Survey") {
        emptyList()
    } else {
        val parent = first { it.code == code }.parent!!
        if (parent == "Survey"){
            emptyList()
        } else {
            parents(parent) + listOf(parent.splitToComponentCodes().last())
        }
    }
}

fun MutableList<ComponentIndex>.sortChildren(
    order: Map<String, Any>
): MutableList<ComponentIndex> {
    val component = first()
    val children = component.children
    if (children.isEmpty()) {
        return this
    }
    val sortedChildren = children.sortedBy {
        order["$it.order"] as? Int ?: (children.indexOf(it) + 1)
    }
    return mutableListOf<ComponentIndex>().apply {
        add(component)
        sortedChildren.map { child ->
            val isLast = children.indexOf(child) == children.size - 1
            val fromIndex = this@sortChildren.indexOfFirst { it.code == child }
            if (fromIndex >= 0) {
                addAll(
                    this@sortChildren.subList(
                        fromIndex = fromIndex,
                        toIndex = if (isLast) {
                            this@sortChildren.size

                        } else {
                            this@sortChildren.indexOfFirst { item ->
                                children.contains(item.code) && children.indexOf(item.code) > children.indexOf(child)
                            }
                        }
                    ).sortChildren(order)
                )
            }
        }
    }
}