package com.frankie.expressionmanager.ext


fun <T> List<List<T>>.flatten(): List<T> {
    return mutableListOf<T>().apply {
        this@flatten.forEach {
            addAll(it)
        }
    }
}

fun <T> List<T>.getDuplicates(): Set<T> {
    return this.groupingBy { it }
        .eachCount()
        .filter { it.value > 1 }
        .keys
}