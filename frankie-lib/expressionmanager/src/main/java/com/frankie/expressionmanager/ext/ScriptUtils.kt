package com.frankie.expressionmanager.ext

class ScriptUtils {
    val commonScript: String
    val engineScript : String

    init {
        val classLoader = javaClass.classLoader
        commonScript = classLoader.getResourceAsStream("scripts/common_script.js")!!.reader().readText()
        val initialScript = classLoader.getResourceAsStream("scripts/initial_script.js")!!.reader().readText()
        engineScript = commonScript +
                "\n" +
                initialScript
    }
}
