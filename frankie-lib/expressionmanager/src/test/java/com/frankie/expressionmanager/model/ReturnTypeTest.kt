package com.frankie.expressionmanager.model

import com.frankie.expressionmanager.common.buildScriptEngine
import com.frankie.expressionmanager.context.build.ContextBuilder
import com.frankie.expressionmanager.model.Instruction.SimpleState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("LocalVariableName")
class ReturnTypeTest {
    @Test
    fun `active instructions with ReturnType Boolean and InputFormatError will default to false`() {

        val QUESTION_ONE = Question(
            "Q1", listOf(
                SimpleState("asdfsad", ReservedCode.ConditionalRelevance)
            )
        )

        val QUESTION_TWO = Question(
            "Q2", listOf(
                SimpleState("!Q1.relevance", ReservedCode.ConditionalRelevance)
            )
        )

        val GROUP = Group("G1", listOf(), listOf(QUESTION_ONE, QUESTION_TWO))
        val contextManager = ContextBuilder(mutableListOf(GROUP), buildScriptEngine())
        contextManager.validate()
        val errorInstruction =
            contextManager.components[0].children[0].instructionList[0].errors[0] as BindingErrors.ScriptFailure

        assertEquals(ResultType.SYNTAX_ERROR, errorInstruction.scriptFailure.resultType)
        assertEquals(true, contextManager.getVariables("Q1.conditional_relevance"))
        assertEquals(false, contextManager.getVariables("Q2.conditional_relevance"))
    }


    @Test
    fun `instructions with ReturnType List can be initialised and later referenced (both active and inactive)`() {
        val QUESTION_ONE = Question(
            "Q1", listOf(
                SimpleState("[1 ,2 ,3]", returnType = ReturnType.FrankieList, reservedCode = ReservedCode.Value)
            )
        )

        val QUESTION_TWO = Question(
            "Q2", listOf(
                SimpleState("Q1.value[0]", returnType = ReturnType.FrankieString, reservedCode = ReservedCode.Value, isActive = true)
            )
        )
        val QUESTION_THREE = Question(
            "Q3", listOf(
                SimpleState(
                    "[\"1\",\"2\",\"3\"]",
                    returnType = ReturnType.FrankieList,
                    reservedCode = ReservedCode.Value,
                    isActive = true
                )
            )
        )
        val QUESTION_FOUR = Question(
            "Q4", listOf(
                SimpleState(
                    "Q3.value.length",
                    returnType = ReturnType.FrankieInt,
                    reservedCode = ReservedCode.Value,
                    isActive = true
                )
            )
        )
        val QUESTION_FIVE = Question(
            "Q5", listOf(
                SimpleState("Q1.value", returnType = ReturnType.FrankieList, reservedCode = ReservedCode.Value, isActive = true)
            )
        )
        val QUESTION_SIX = Question(
            "Q6", listOf(
                SimpleState(
                    "Q5.value.length",
                    returnType = ReturnType.FrankieInt,
                    reservedCode = ReservedCode.Value,
                    isActive = true
                )
            )
        )

        val GROUP = Group(
            "G1",
            listOf(),
            listOf(QUESTION_ONE, QUESTION_TWO, QUESTION_THREE, QUESTION_FOUR, QUESTION_FIVE, QUESTION_SIX)
        )
        val contextManager = ContextBuilder(mutableListOf(GROUP), buildScriptEngine())
        contextManager.validate()
        assertEquals(JSONArray("[1 ,2 ,3]").toString(), contextManager.getVariables("Q1.value").toString())
        assertEquals(1, contextManager.getVariables("Q2.value"))
        assertEquals(JSONArray("[\"1\",\"2\",\"3\"]").toString(), contextManager.getVariables("Q3.value").toString())
        assertEquals(3, contextManager.getVariables("Q4.value"))
        assertEquals(JSONArray("[1 ,2 ,3]").toString(), contextManager.getVariables("Q5.value").toString())
        assertEquals(3, contextManager.getVariables("Q6.value"))
    }

    @Test
    fun `instructions with an inactive ReturnType will return a default value in a Dynamic Instruction`() {
        val QUESTION_ONE = Question(
            "Q1", listOf(
                SimpleState(
                    "{\"firstName\":\"John\", \"lastName\":\"Doe\"}",
                    returnType = ReturnType.FrankieMap,
                    reservedCode = ReservedCode.Value,
                    isActive = true
                )
            )
        )

        val contextManager = ContextBuilder(mutableListOf(QUESTION_ONE.wrapToSurvey()), buildScriptEngine())
        contextManager.validate()
        val expected = JSONObject().apply {
            put("firstName", "John")
            put("lastName", "Doe")
        }
        assertEquals(expected.toString(), contextManager.getVariables("Q1.value").toString())
    }

    @Test
    fun `instructions wdith an inactive ReturnType will return a default value in a Dynamic Instruction`() {
        val QUESTION_ONE = Question(
            "Q1", listOf(
                SimpleState(
                    "{\"firstName\":\"John\", \"lastName\":\"Doe\",\"numbers\":[\"1\", \"2\", \"3\"]}",
                    returnType = ReturnType.FrankieMap,
                    reservedCode = ReservedCode.Value
                )
            )
        )

        val QUESTION_TWO = Question(
            "Q2", listOf(
                SimpleState(
                    "Q1.value['firstName']",
                    returnType = ReturnType.FrankieString,
                    reservedCode = ReservedCode.Value,
                    isActive = true
                )
            )
        )

        val QUESTION_THREE = Question(
            "Q3", listOf(
                SimpleState("Q1.value", returnType = ReturnType.FrankieMap, reservedCode = ReservedCode.Value, isActive = true)
            )
        )


        val QUESTION_FOUR = Question(
            "Q4", listOf(
                SimpleState(
                    "Q1.value['numbers']",
                    returnType = ReturnType.FrankieList,
                    reservedCode = ReservedCode.Value,
                    isActive = true
                )
            )
        )


        val QUESTION_FIVE = Question(
            "Q5", listOf(
                SimpleState("Q1.value['numbers']", returnType = ReturnType.FrankieMap, reservedCode = ReservedCode.Value)
            )
        )

        val QUESTION_SIX = Question(
            "Q6", listOf(
                SimpleState(
                    "{\"S1\": {\"name\":\"Meat\"},\"S2\": {\"name\":\"Chicken\"},\"S3\": {\"name\":\"Pasta\"}}",
                    returnType = ReturnType.FrankieMap,
                    reservedCode = ReservedCode.Value
                )
            )
        )

        val GROUP = Group(
            "G1",
            listOf(),
            listOf(QUESTION_ONE, QUESTION_TWO, QUESTION_THREE, QUESTION_FOUR, QUESTION_FIVE, QUESTION_SIX)
        )
        val contextManager = ContextBuilder(mutableListOf(GROUP), buildScriptEngine())
        contextManager.validate()
        assertEquals(
            "{\"S3\":{\"name\":\"Pasta\"},\"S1\":{\"name\":\"Meat\"},\"S2\":{\"name\":\"Chicken\"}}",
            contextManager.getVariables("Q6.value").toString()
        )
        assertEquals("John", contextManager.getVariables("Q2.value").toString())
        assertEquals("[\"1\",\"2\",\"3\"]", contextManager.getVariables("Q4.value").toString())
    }
}