package com.frankie.expressionmanager.dependency

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ComponentIndexExt {

    private fun componentIndices(): List<ComponentIndex> {
        val filesPath = "${System.getProperty("user.dir")}/src/test/resources/component_indices.json"
        val componentIndicesJson = File(filesPath).inputStream().readBytes().toString(Charsets.UTF_8)
        return jacksonKtMapper.readValue(componentIndicesJson, jacksonTypeRef<List<ComponentIndex>>())
    }


    @Test
    fun `indices are resolved with random groups`() {
        val instruction = Instruction.RandomGroups(
            listOf(
                listOf("G1", "G2", "G3", "G4"),
                listOf("G5", "G6", "G7", "G8"),
                listOf("G9", "G10", "G11", "G12")
            )
        )
        val survey = Survey(
            groups = listOf(
                Group("G1"), Group("G2"), Group("G3"), Group("G4"),
                Group("G5"), Group("G6"), Group("G7"), Group("G8"),
                Group("G9"), Group("G10"), Group("G11"), Group("G12")
            ),
            instructionList = listOf(instruction)

        )
        assertEquals(listOf(0, 0, 0, 0, 4, 4, 4, 4, 8, 8, 8, 8), survey.children.indices(instruction).first)
    }

    @Test
    fun `indices are resolved with random groups non linear`() {
        val instruction = Instruction.RandomGroups(
            listOf(
                listOf("G1", "G5", "G9"),
                listOf("G2", "G6", "G10")
            )
        )
        val survey = Survey(
            groups = listOf(
                Group("G1"), Group("G2"), Group("G3"), Group("G4"),
                Group("G5"), Group("G6"), Group("G7"), Group("G8"),
                Group("G9"), Group("G10"), Group("G11"), Group("G12")
            ),
            instructionList = listOf(instruction)

        )
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 6, 7, 0, 1, 10, 11), survey.children.indices(instruction).first)
    }


    @Test
    fun `componentIndices are resolved`() {
        val surveyRandom = Instruction.RandomGroups(
            listOf(
                listOf("G2", "G3", "G4")
            )
        )
        val groupPriority = Instruction.RandomGroups(
            listOf(
                listOf("Q2", "Q4", "Q5")
            )
        )
        val groupOne = Group(
            code = "G1",
            questions = listOf(Question("Q1"), Question("Q2"), Question("Q3"), Question("Q4"), Question("Q5")),
            instructionList = listOf(groupPriority)
        )
        val survey = Survey(
            groups = listOf(
                groupOne, Group("G2"), Group("G3"), Group("G4"), Group("G5")
            ),
            instructionList = listOf(surveyRandom)

        )
        assertEquals(
            listOf(
                Pair("Survey", 0),
                Pair("G1", 0),
                Pair("Q1", 0),
                Pair("Q2", 1),
                Pair("Q3", 2),
                Pair("Q4", 1),
                Pair("Q5", 1),
                Pair("G2", 1),
                Pair("G3", 1),
                Pair("G4", 1),
                Pair("G5", 4)
            ), listOf(survey).componentIndices().map { Pair(it.code, it.minIndex) }
        )
    }

    @Test
    fun `componentIndices are resolved2`() {
        val groupRandom = Instruction.RandomGroups(
            listOf(
                listOf("Q2", "Q3", "Q4")
            )
        )
        val questionPriority = Instruction.RandomGroups(
            listOf(
                listOf("A2", "A4", "A5")
            )
        )
        val questionOne = Question(
            code = "Q1",
            answers = listOf(Answer("A1"), Answer("A2"), Answer("A3"), Answer("A4"), Answer("A5")),
            instructionList = listOf(questionPriority)
        )
        val group = Group(
            code = "G1",
            questions = listOf(
                questionOne, Question("Q2"), Question("Q3"), Question("Q4"), Question("Q5")
            ),
            instructionList = listOf(groupRandom)

        )
        val accessibleDependencies = listOf(group).componentIndices()
        assertEquals(
            listOf(
                Pair("G1", 0),
                Pair("Q1", 0),
                Pair("Q1A1", 0),
                Pair("Q1A2", 1),
                Pair("Q1A3", 2),
                Pair("Q1A4", 1),
                Pair("Q1A5", 1),
                Pair("Q2", 1),
                Pair("Q3", 1),
                Pair("Q4", 1),
                Pair("Q5", 4),
            ), accessibleDependencies.map { Pair(it.code, it.minIndex) }
        )
    }

    @Test
    fun `accessibleDependencies are resolved`() {
        val componentIndices = componentIndices()
        assertEquals(
            listOf(
                "Q1", "Q1Afirst_name", "Q1Alast_name", "Q1Aemail", "Q1Agender", "Q1AgenderAmale",
                "Q1AgenderAfemale", "Q1Abirthday", "Q2", "Q2A1", "Q2A2", "Q2A3", "Q3"
            ),
            componentIndices().accessibleDependencies("Q3Afood").componentCodes()
        )
        assertEquals(
            listOf(
                "G1",
                "Q1",
                "Q1Afirst_name",
                "Q1Alast_name",
                "Q1Aemail",
                "Q1Agender",
                "Q1AgenderAmale",
                "Q1AgenderAfemale",
                "Q1Abirthday",
                "Q2",
                "Q2A1",
                "Q2A2",
                "Q2A3",
                "Q3",
                "Q3Afood",
                "Q3Aservice",
                "Q3Avenue",
                "Q3Aother",
                "Q3AotherAtext",
                "Q4",
                "Q4Ac1",
                "Q4Ac2",
                "Q4Ac3",
                "Q4Afood",
                "Q4Aservice",
                "Q4Avenue",
                "Q5",
                "Q5Ac1",
                "Q5Ac2",
                "Q5A1",
                "Q5A1A1",
                "Q5A1A2",
                "Q5A1A3",
                "Q5A2",
                "Q5A2A1",
                "Q5A2A2"
            ), componentIndices().accessibleDependencies("Gfood").componentCodes()
        )
        assertEquals(
            componentIndices.accessibleDependencies("Survey").componentCodes()
                .toMutableList().apply { remove("G5") },
            componentIndices().accessibleDependencies("G5").componentCodes()
        )


    }

    private fun List<Dependency>.componentCodes(): List<String> = map { it.componentCode }.distinct()
}