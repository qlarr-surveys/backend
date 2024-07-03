package com.frankie.expressionmanager.validation

import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.model.ReservedCode
import com.frankie.expressionmanager.model.SurveyLang
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.ext.copyErrorsToJSON
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.SimpleState
import org.junit.Assert.assertEquals
import org.junit.Test

class CopyToJSONTest {

    @Test
    fun `copies instructions and errors`() {
        val component = Survey(
            instructionList = listOf(SimpleState("", ReservedCode.Value)),
            errors = listOf(ComponentError.DUPLICATE_CODE)
        )
        val json = jacksonKtMapper.readTree("{\"code\":\"Survey\"}")
        assertEquals(
            "{\"code\":\"Survey\",\"qualifiedCode\":\"Survey\",\"instructionList\":[{\"code\":\"value\",\"text\":\"\",\"returnType\":{\"name\":\"String\"},\"isActive\":false}],\"errors\":[\"DUPLICATE_CODE\"]}",
            component.copyErrorsToJSON(json as ObjectNode).toString()
        )
    }

    @Test
    fun `copies instructions and errors2`() {
        val component = Survey(
            instructionList = listOf(
                Instruction.Reference(
                    "reference_1",
                    references = emptyList(),
                    lang = SurveyLang.EN.code,
                    errors = listOf(
                        BindingErrors.ScriptFailure(
                            ScriptResult(
                                ResultType.SYNTAX_ERROR,
                                "SyntaxError: Unexpected identifier"
                            )
                        )
                    )
                )
            )
        )
        val json = jacksonKtMapper.readTree("{\"code\":\"Survey\"}")
        assertEquals(
            "{\"code\":\"Survey\",\"qualifiedCode\":\"Survey\",\"instructionList\":[{\"code\":\"reference_1\",\"references\":[],\"lang\":\"en\",\"errors\":[{\"scriptFailure\":{\"resultType\":\"SYNTAX_ERROR\",\"valueOrError\":\"SyntaxError: Unexpected identifier\"},\"name\":\"ScriptFailure\"}]}]}",
            component.copyErrorsToJSON(json as ObjectNode).toString()
        )
    }

    @Test
    fun `overrides json existing instructions and errors`() {
        val component = Survey(
            instructionList = listOf(SimpleState("", ReservedCode.Value))
        )
        val json =
            jacksonKtMapper.readTree("{\"code\":\"Survey\",\"instructionList\":[{\"code\":\"conditional_relevance\",\"text\":\"false\",\"isActive\":false,\"returnType\":\"Boolean\"}]}")
        assertEquals(
            "{\"code\":\"Survey\",\"instructionList\":[{\"code\":\"value\",\"text\":\"\",\"returnType\":{\"name\":\"String\"},\"isActive\":false}],\"qualifiedCode\":\"Survey\"}",
            component.copyErrorsToJSON(json as ObjectNode).toString()
        )
    }

    @Test
    fun `keeps json object other values intact`() {
        val component = Survey(
            instructionList = listOf(SimpleState("", ReservedCode.Value)),
            errors = listOf(ComponentError.DUPLICATE_CODE)
        )
        val json = jacksonKtMapper.readTree("{\"code\":\"Survey\",\"foo\":\"bar\"}")
        assertEquals("bar", component.copyErrorsToJSON(json as ObjectNode).get("foo").textValue())
    }

    @Test
    fun `copies instructions and errors to nested children provided same code`() {
        val component = Survey(
            groups = listOf(
                Group("G1", listOf(SimpleState("G1", ReservedCode.Value))),
                Group("G2", listOf(SimpleState("G2", ReservedCode.Value)))
            )
        )
        val json =
            jacksonKtMapper.readTree("{\"code\":\"Survey\",\"groups\":[{\"code\":\"G1\"},{\"code\":\"G2\"},{\"code\":\"G3\"}]}")
        assertEquals(
            "{\"code\":\"Survey\",\"groups\":[{\"code\":\"G1\",\"qualifiedCode\":\"G1\",\"instructionList\":[{\"code\":\"value\",\"text\":\"G1\",\"returnType\":{\"name\":\"String\"},\"isActive\":false}]},{\"code\":\"G2\",\"qualifiedCode\":\"G2\",\"instructionList\":[{\"code\":\"value\",\"text\":\"G2\",\"returnType\":{\"name\":\"String\"},\"isActive\":false}]}],\"qualifiedCode\":\"Survey\"}",
            component.copyErrorsToJSON(json as ObjectNode).toString()
        )
    }

}