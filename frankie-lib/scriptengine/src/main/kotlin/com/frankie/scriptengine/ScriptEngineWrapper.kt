package com.frankie.scriptengine

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import javax.script.Bindings
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptEngine


class ScriptEngineWrapper(
    script: String
) {

    private val compiledScript: CompiledScript

    init {
        compiledScript = (engine as Compilable).compile(
            script +
                    "\n" +
                    "if (method == 'validate') {" +
                    "   validate(JSON.parse(params)) " +
                    "} " +
                    "else {" +
                    "   navigate(JSON.parse(params)) " +
                    "}"
        )
    }

    companion object {
        var engine: ScriptEngine = GraalJSScriptEngine.create(null,
            Context.newBuilder("js")
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup { false }
                .option("js.ecmascript-version", "2021"))

    }

    fun executeScript(method: String, script: String): String {
        val scriptParams: Bindings = engine.createBindings()
        scriptParams["method"] = method
        scriptParams["params"] = script
        return compiledScript.eval(scriptParams).toString()
    }

}