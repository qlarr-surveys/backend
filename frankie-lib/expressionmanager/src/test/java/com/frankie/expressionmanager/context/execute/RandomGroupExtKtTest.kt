package com.frankie.expressionmanager.context.execute

import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.model.Instruction.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

@Suppress("UNCHECKED_CAST")
class RandomGroupExtKtTest {

    @Test
    fun randomize() {
        val question = Question(
            code = "Q1",
            instructionList = listOf(RandomGroups(listOf(listOf("A1", "A2", "A3"), listOf("A4", "A5", "A6")))),
            answers = listOf(
                Answer(
                    code = "A1",
                    instructionList = listOf(RandomGroups(listOf(listOf("A1", "A2", "A3", "A4", "A5", "A6")))),
                    answers = listOf(
                        Answer("A1"),
                        Answer("A2"),
                        Answer("A3"),
                        Answer("A4"),
                        Answer("A5"),
                        Answer("A6")
                    )
                ),
                Answer("A2"),
                Answer("A3"),
                Answer("A4"),
                Answer("A5"),
                Answer("A6")
            )
        )
        val originalList = mutableListOf(question)
        val randomized = originalList.randomizeChildren()
        assertNotEquals(
            listOf(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6),
            randomized.orderValues(
                listOf(
                    "Q1A1",
                    "Q1A2",
                    "Q1A3",
                    "Q1A4",
                    "Q1A5",
                    "Q1A6",
                    "Q1A1A1",
                    "Q1A1A2",
                    "Q1A1A3",
                    "Q1A1A4",
                    "Q1A1A5",
                    "Q1A1A6"
                )
            )
        )
        assertEquals(
            setOf(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6),
            randomized.orderValues(
                listOf(
                    "Q1A1",
                    "Q1A2",
                    "Q1A3",
                    "Q1A4",
                    "Q1A5",
                    "Q1A6",
                    "Q1A1A1",
                    "Q1A1A2",
                    "Q1A1A3",
                    "Q1A1A4",
                    "Q1A1A5",
                    "Q1A1A6"
                )
            ).toSet()
        )
    }

    @Test
    fun randomizeInPendulum() {
        val question = Question(
            code = "Q1",
            instructionList = listOf(
                RandomGroups(
                    listOf(
                        RandomGroup(
                            listOf("A1", "A2", "A3"),
                            RandomOption.FLIP
                        )
                    )
                )
            ),
            answers = listOf(
                Answer("A1"),
                Answer("A2"),
                Answer("A3")
            )
        )
        val originalList = mutableListOf(question)
        val set = mutableSetOf<List<Int>>()
        for (i in 0 until 20) {
            set.add(originalList.randomizeChildren().orderValues(listOf("Q1A1", "Q1A2", "Q1A3")))
        }

        assertEquals(setOf(listOf(1, 2, 3), listOf(3, 2, 1)), set)
    }


    @Test
    fun randomizeInAlpha() {
        val question = Question(
            code = "Q1",
            instructionList = listOf(
                RandomGroups(
                    listOf(
                        RandomGroup(
                            listOf("A1", "A2", "A3"),
                            RandomOption.ALPHA
                        )
                    )
                )
            ),
            answers = listOf(
                Answer("A1"),
                Answer("A2"),
                Answer("A3")
            )
        )
        val originalList = mutableListOf(question)
        val set = mutableSetOf<List<Int>>()
        val getLabel: (String) -> String = { code ->
            when (code) {
                "Q1A1" -> "B"
                "Q1A2" -> "A"
                "Q1A3" -> "C"
                else -> throw Exception()
            }
        }

        val getARLabel: (String) -> String = { code ->
            when (code) {
                "Q1A1" -> "ب"
                "Q1A2" -> "ج"
                "Q1A3" -> "ا"
                else -> throw Exception()
            }
        }
        for (i in 0 until 20) {
            set.add(originalList.randomizeChildren(getLabel = getLabel).orderValues(listOf("Q1A1", "Q1A2", "Q1A3")))
        }
        assertEquals(setOf(listOf(2, 1, 3)), set)

        set.clear()

        for (i in 0 until 20) {
            set.add(originalList.randomizeChildren(getLabel = getARLabel).orderValues(listOf("Q1A1", "Q1A2", "Q1A3")))
        }
        assertEquals(setOf(listOf(3, 1, 2)), set)
    }


    @Test
    fun prioritises() {
        val question = Question(
            code = "Q1",
            instructionList = listOf(
                PriorityGroups(
                    listOf(
                        PriorityGroup(listOf("A1", "A2", "A3")),
                        PriorityGroup(listOf("A4", "A5", "A6"))
                    )
                )
            ),
            answers = listOf(
                Answer(
                    code = "A1",
                    instructionList = listOf(
                        PriorityGroups(
                            listOf(
                                PriorityGroup(
                                    listOf(
                                        "A1",
                                        "A2",
                                        "A3",
                                        "A4",
                                        "A5",
                                        "A6"
                                    )
                                )
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
                ),
                Answer("A2"),
                Answer("A3"),
                Answer("A4"),
                Answer("A5"),
                Answer("A6")
            )
        )
        val originalList = mutableListOf(question) as MutableList<SurveyComponent>
        val randomized = originalList.setChildrenPriorities()
        assertNotEquals(
            listOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 4, 5, 6),
            randomized.priorityValues(
                listOf(
                    "Q1A1",
                    "Q1A2",
                    "Q1A3",
                    "Q1A4",
                    "Q1A5",
                    "Q1A6",
                    "Q1A1A1",
                    "Q1A1A2",
                    "Q1A1A3",
                    "Q1A1A4",
                    "Q1A1A5",
                    "Q1A1A6"
                )
            )
        )
        assertEquals(
            setOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 4, 5, 6),
            randomized.priorityValues(
                listOf(
                    "Q1A1",
                    "Q1A2",
                    "Q1A3",
                    "Q1A4",
                    "Q1A5",
                    "Q1A6",
                    "Q1A1A1",
                    "Q1A1A2",
                    "Q1A1A3",
                    "Q1A1A4",
                    "Q1A1A5",
                    "Q1A1A6"
                )
            ).toSet()
        )
    }
}

fun Map<Dependency, Int>.orderValues(list: List<String>): List<Int> {
    return list.mapNotNull { get(Dependency(it, ReservedCode.Order)) }
}

fun Map<Dependency, Int>.priorityValues(list: List<String>): List<Int> {
    return list.mapNotNull { get(Dependency(it, ReservedCode.Priority)) }
}