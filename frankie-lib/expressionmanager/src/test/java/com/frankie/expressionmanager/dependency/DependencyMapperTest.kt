package com.frankie.expressionmanager.dependency

import com.frankie.expressionmanager.common.buildScriptEngine
import com.frankie.expressionmanager.model.ReservedCode
import com.frankie.expressionmanager.model.SurveyLang
import com.frankie.expressionmanager.context.build.ContextBuilder
import com.frankie.expressionmanager.model.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("PrivatePropertyName")
class DependencyMapperTest {

    private val QUESTION_Q1 = Question(
        code = "Q1",
        instructionList = listOf(
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance),
            Instruction.SimpleState("", ReservedCode.Value)
        )
    )

    private val QUESTION_Q2 = Question(
        code = "Q2",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("Q1.value=='BAD'", ReservedCode.ConditionalRelevance)
        )
    )

    private val QUESTION_Q3 = Question(
        code = "Q3",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("!Q2.conditional_relevance", ReservedCode.ConditionalRelevance)
        )
    )

    private val GROUP_G1 = Group(
        code = "G1",
        instructionList = listOf(
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance)
        ),
        questions = listOf(QUESTION_Q1, QUESTION_Q2, QUESTION_Q3)
    )

    private val QUESTION_Q4 = Question(
        code = "Q4",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance),
            Instruction.SimpleState("", ReservedCode.Label)
        )
    )

    private val QUESTION_Q5 = Question(
        code = "Q5",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("Q4.value!=''", ReservedCode.ConditionalRelevance)
        ),
        answers = listOf(
            Answer(
                "A1",
                instructionList = listOf(
                    Instruction.SimpleState(
                        "Q4.conditional_relevance",
                        ReservedCode.ConditionalRelevance
                    )
                )
            )
        )
    )

    private val GROUP_G2 = Group(
        code = "G2",
        instructionList = listOf(
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance)
        ),
        questions = listOf(QUESTION_Q4, QUESTION_Q5)
    )


    private val QUESTION_Q6 = Question(
        code = "Q6",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance)
        )
    )

    private val QUESTION_Q7 = Question(
        code = "Q7",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance)
        )
    )

    private val GROUP_G3 = Group(
        code = "G3",
        instructionList = listOf(
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance)
        ),
        questions = listOf(QUESTION_Q6, QUESTION_Q7)
    )


    private val QUESTION_Q8 = Question(
        code = "Q8",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance)
        )
    )

    private val QUESTION_Q9 = Question(
        code = "Q9",
        instructionList = listOf(
            Instruction.SimpleState("", ReservedCode.Value),
            Instruction.SimpleState("Q8.value!=''", ReservedCode.ConditionalRelevance),
            Instruction.Reference("reference_label", listOf("Q4.label"), SurveyLang.EN.code)
        )
    )

    private val GROUP_G4 = Group(
        code = "G4",
        instructionList = listOf(
            Instruction.SimpleState("true", ReservedCode.ConditionalRelevance)
        ),
        questions = listOf(QUESTION_Q8, QUESTION_Q9)
    )


    private var contextBuilder: ContextBuilder = ContextBuilder(scriptEngine = buildScriptEngine())
    private var dependencyMapper: DependencyMapper = DependencyMapper()

    @Before
    fun setup() {
        contextBuilder = ContextBuilder(mutableListOf(GROUP_G1, GROUP_G2, GROUP_G3, GROUP_G4), buildScriptEngine())
        dependencyMapper = DependencyMapper(contextBuilder.sanitizedNestedComponents)
    }

    @Test
    fun `dependencyMap has complete set of dependencies`() {
        val expected = mapOf(
            Dependent(componentCode = "Q2", instructionCode = "conditional_relevance") to listOf(
                Dependency(
                    componentCode = "Q1",
                    reservedCode = ReservedCode.Value
                )
            ),
            Dependent(componentCode = "Q3", instructionCode = "conditional_relevance") to listOf(
                Dependency(
                    componentCode = "Q2",
                    reservedCode = ReservedCode.ConditionalRelevance
                )
            ),
            Dependent(componentCode = "Q5", instructionCode = "conditional_relevance") to listOf(
                Dependency(
                    componentCode = "Q4",
                    reservedCode = ReservedCode.Value
                )
            ),
            Dependent(componentCode = "Q5A1", instructionCode = "conditional_relevance") to listOf(
                Dependency(
                    componentCode = "Q4",
                    reservedCode = ReservedCode.ConditionalRelevance
                )
            ),
            Dependent(
                componentCode = "Q9",
                instructionCode = "reference_label"
            ) to listOf(Dependency(componentCode = "Q4", reservedCode = ReservedCode.Label)),

            Dependent(componentCode = "Q9", instructionCode = "conditional_relevance") to listOf(
                Dependency(
                    componentCode = "Q8",
                    reservedCode = ReservedCode.Value
                )
            )
        )
        assertEquals(expected, dependencyMapper.dependencyMap.toSortedMap())

    }

    @Test
    fun `Dependency Mapper has identical maps whether initialised from components or StringMap`() {
        val newDependencyMapper = DependencyMapper(dependencyMapper.impactMap.toStringImpactMap())
        assertEquals(newDependencyMapper.impactMap.toSortedMap(), dependencyMapper.impactMap.toSortedMap())
        assertEquals(newDependencyMapper.dependencyMap.toSortedMap(), dependencyMapper.dependencyMap.toSortedMap())
    }

    @Test
    fun `impactsMap has complete set of dependents`() {
        val expected = mapOf(
            Dependency("Q1", ReservedCode.Value) to listOf(Dependent("Q2", "conditional_relevance")),
            Dependency("Q2", ReservedCode.ConditionalRelevance) to listOf(Dependent("Q3", "conditional_relevance")),
            Dependency("Q4", ReservedCode.ConditionalRelevance) to listOf(Dependent("Q5A1", "conditional_relevance")),
            Dependency("Q4", ReservedCode.Label) to listOf(Dependent("Q9", "reference_label")),
            Dependency("Q4", ReservedCode.Value) to listOf(Dependent("Q5", "conditional_relevance")),
            Dependency("Q8", ReservedCode.Value) to listOf(Dependent("Q9", "conditional_relevance"))
        )
        assertEquals(expected, dependencyMapper.impactMap.toSortedMap())

    }

    @Test
    fun `getDependants return empty map if no dependents exist`() {
        val expected = mapOf<String, List<Dependent>>()
        val dependencyMapper = DependencyMapper(listOf())
        assertEquals(expected, dependencyMapper.impactMap)
    }

}
