package com.frankie.expressionmanager.usecase

interface ScriptEngine {
    fun executeScript(method: String, script: String): String
}