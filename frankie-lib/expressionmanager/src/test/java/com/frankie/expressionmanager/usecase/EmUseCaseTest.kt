package com.frankie.expressionmanager.usecase

import com.frankie.expressionmanager.common.buildScriptEngine
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.SimpleState
import org.junit.Assert.assertEquals
import org.junit.Test

class EmUseCaseTest {

    @Test
    fun `script errors are reflected`() {
        val questionComponent = Question(
            code = "Q1",
            instructionList = listOf(SimpleState(";;;getdaSD dasd", ReservedCode.Value, isActive = true))
        ).wrapToSurvey()
        val survey = ValidationUseCaseImpl(buildScriptEngine(), questionComponent).validate(false).survey


        assert(survey.groups[0].questions[0].instructionList[0].errors[0] is BindingErrors.ScriptFailure)
        assert((survey.groups[0].questions[0].instructionList[0].errors[0] as BindingErrors.ScriptFailure).scriptFailure.resultType == ResultType.SYNTAX_ERROR)
    }

    @Test
    fun `fwd reference errors are reflected`() {
        val questionComponent1 = Question(
            code = "Q1",
            instructionList = listOf(SimpleState("Q2.value", ReservedCode.Value, isActive = true))
        )
        val questionComponent2 = Question(
            code = "Q2",
            instructionList = listOf(SimpleState("Q1.value", ReservedCode.Value, isActive = true))
        )
        val component = Group("G1", questions = listOf(questionComponent1, questionComponent2)).wrapToSurvey()
        val survey = ValidationUseCaseImpl(buildScriptEngine(), component).validate(false).survey
        assertEquals(
            BindingErrors.ForwardDependency(
                Dependency("Q2", ReservedCode.Value)
            ), survey.groups[0].questions[0].instructionList[0].errors[0]
        )
    }
}
