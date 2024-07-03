package com.frankie.expressionmanager.common

import com.frankie.expressionmanager.ext.ScriptUtils
import com.frankie.expressionmanager.model.SurveyComponent
import com.frankie.expressionmanager.usecase.ScriptEngine
import com.frankie.scriptengine.ScriptEngineWrapper

fun SurveyComponent.getErrorsCount(): Int {
    var returnResult = errors.size
    instructionList.forEach { instruction ->
        returnResult += instruction.errors.size
    }
    children.forEach { component ->
        returnResult += component.getErrorsCount()
    }

    return returnResult
}

fun buildScriptEngine(): ScriptEngine {
    val scriptEngineWrapper = ScriptEngineWrapper(ScriptUtils().engineScript)
    return object : ScriptEngine {
        override fun executeScript(method: String, script: String): String {
            return scriptEngineWrapper.executeScript(method, script)
        }

    }
}
