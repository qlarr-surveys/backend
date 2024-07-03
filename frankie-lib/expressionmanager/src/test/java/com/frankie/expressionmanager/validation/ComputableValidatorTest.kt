package com.frankie.expressionmanager.validation

import com.frankie.expressionmanager.model.ReservedCode
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.*
import org.junit.Assert.assertEquals
import org.junit.Test


class ComponentValidatorTest {


    @Test
    fun `validates special type groups`() {
        val survey_ok = Survey(
            groups = listOf(
                Group("G2"),
                Group("G3"),
                Group("G4"),
                Group("G5", groupType = GroupType.END, questions = listOf(Question("Q1", answers = listOf(Answer("A1", instructionList = listOf(
                    SimpleState("", ReservedCode.Value)
                )))))),
            )
        )
        val survey_not_ok = Survey(
            groups = listOf(
                Group("G1"),
                Group("G3"),
                Group("G4", groupType = GroupType.END),
                Group("G5"),
            )
        )
        val validated_ok = listOf(survey_ok).validateSpecialTypeGroups()[0]
        assertEquals(
            emptyList<ComponentError>(),
            validated_ok.children.map { it.errors }.flatten()
        )
        assertEquals(
            BindingErrors.InvalidInstructionInEndGroup,
            validated_ok.children[3].children[0].children[0].instructionList[0].errors[0]
        )
        val validated_not_ok = listOf(survey_not_ok).validateSpecialTypeGroups()[0]
        assertEquals(ComponentError.MISPLACED_END_GROUP, validated_not_ok.children[2].errors[0])

    }

    @Test
    fun `Random Group Equations are validated`() {
        val QUESTION_ONE = Question(
            "Q1",
            instructionList = listOf(
                RandomGroups(
                    listOf(
                        listOf("A1", "A2"),
                        listOf("A4", "A5"),
                        listOf("A4", "A8", "A9")
                    )
                )
            ),
            answers = listOf(
                Answer("A1"),
                Answer("A2"),
                Answer("A3"),
                Answer("A4"),
                Answer("A5"),
                Answer("A6")
            )
        )

        val components = listOf(QUESTION_ONE)
        val validated = components.map { it.validateInstructions() }
        assertEquals(BindingErrors.DuplicateRandomGroupItems(listOf("A4")), validated[0].instructionList[0].errors[0])
        assertEquals(
            BindingErrors.RandomGroupItemNotChild(listOf("A8", "A9")),
            validated[0].instructionList[0].errors[1]
        )
    }

    @Test
    fun `priority and random groups validates against welcome and end groups`() {
        val SURVEY = Survey(
            instructionList = listOf(
                PriorityGroups(
                    listOf(
                        PriorityGroup(listOf("G3", "G4")),
                        PriorityGroup(listOf("G5", "G6", "G7"))
                    )
                ),
                RandomGroups(listOf(
                    listOf("G3", "G4"),
                    listOf("G5", "G6", "G7")
                ))
            ),
            groups = listOf(
                Group("G2"),
                Group("G3"),
                Group("G4"),
                Group("G5"),
                Group("G6"),
                Group("G7", groupType = GroupType.END)
            )
        )

        val components = listOf(SURVEY)
        val validated = components.map { it.validateInstructions() }
        assertEquals(BindingErrors.InvalidRandomItem    (listOf("G7")), validated[0].instructionList[1].errors[0])
    }
    @Test
    fun `Priority Group Equations are validated`() {
        val QUESTION_ONE = Question(
            "Q1",
            instructionList = listOf(
                PriorityGroups(
                    listOf(
                        PriorityGroup(listOf("A1", "A2")),
                        PriorityGroup(listOf("A4", "A5")),
                        PriorityGroup(listOf("A4", "A8", "A9"))
                    )

                )
            ),
            answers = listOf(
                Answer("A1"),
                Answer("A2"),
                Answer("A3"),
                Answer("A4"),
                Answer("A5"),
                Answer("A6")
            )
        )

        val components = listOf(QUESTION_ONE)
        val validated = components.map { it.validateInstructions() }
        assertEquals(BindingErrors.DuplicatePriorityGroupItems(listOf("A4")), validated[0].instructionList[0].errors[0])
        assertEquals(
            BindingErrors.PriorityGroupItemNotChild(listOf("A8", "A9")),
            validated[0].instructionList[0].errors[1]
        )
    }

    @Test
    fun `Parent conditional_relevance Equations are validated`() {
        val QUESTION_ONE = Question(
            "Q1",
            instructionList = listOf(
                ParentRelevance(listOf(listOf("A1", "A2", "A4", "A5", "A9")))
            ),
            answers = listOf(
                Answer("A1"),
                Answer("A2"),
                Answer("A3"),
                Answer("A4"),
                Answer("A5"),
                Answer("A6")
            )
        )

        val components = listOf(QUESTION_ONE)
        val validated = components.map { it.validateInstructions() }
        assertEquals(BindingErrors.InvalidChildReferences(listOf("A9")), validated[0].instructionList[0].errors[0])
    }
}
