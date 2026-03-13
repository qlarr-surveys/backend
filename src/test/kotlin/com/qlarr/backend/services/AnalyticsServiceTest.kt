package com.qlarr.backend.services

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.api.response.*
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.api.survey.Usage
import com.qlarr.backend.api.surveyengine.ValidationJsonOutput
import com.qlarr.backend.persistence.entities.AnalyticsResponseCount
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.backend.persistence.entities.SurveyNavigationData
import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.model.ComponentIndex
import com.qlarr.surveyengine.model.SurveyLang
import com.qlarr.surveyengine.model.exposed.ColumnName
import com.qlarr.surveyengine.model.exposed.ResponseField
import com.qlarr.surveyengine.model.exposed.ReturnType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class AnalyticsServiceTest {

    @MockK
    private lateinit var designService: DesignService

    @MockK
    private lateinit var responseRepository: ResponseRepository

    @InjectMockKs
    private lateinit var analyticsService: AnalyticsService

    private val surveyId = UUID.randomUUID()
    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `getAnalytics uses latest survey version`() {
        val processed = buildProcessedSurvey(emptyList(), emptyList())
        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns emptyList()
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(0)

        analyticsService.getAnalytics(surveyId)

        verify(exactly = 1) { designService.getProcessedSurvey(surveyId, false) }
    }

    @Test
    fun `getAnalytics returns correct survey title and total responses`() {
        val processed = buildProcessedSurvey(emptyList(), emptyList())
        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns emptyList()
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(42)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals("Test Survey", result.surveyTitle)
        assertEquals(42, result.totalResponses)
        assertTrue(result.questions.isEmpty())
    }

    @Test
    fun `getAnalytics handles SCQ question with label resolution`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Enum(setOf("A1", "A2")), "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", listOf("Q1A1", "Q1A2"))
        )
        val surveyJson = buildSurveyJson("Q1", "SCQ", mapOf(
            "Q1" to "Favorite Color",
            "Q1A1" to "Red",
            "Q1A2" to "Blue"
        ))
        val labels = mapOf("Q1" to "Favorite Color", "Q1A1" to "Red", "Q1A2" to "Blue")

        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to "A1")),
            buildResponse(mapOf("Q1.value" to "A2"))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(2)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("Q1", question.id)
        assertEquals("SCQ", question.type)
        assertEquals("Favorite Color", question.title)
        assertEquals(listOf(AnalyticsOption("A1", "Red"), AnalyticsOption("A2", "Blue")), question.options)
        assertNull(question.responses)
        assertEquals(
            listOf(FrequencyCount("A1", 1), FrequencyCount("A2", 1)),
            question.frequencyCounts
        )
    }

    @Test
    fun `getAnalytics handles MCQ question with multiple selections`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.List(setOf("A1", "A2", "A3")), "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", listOf("Q1A1", "Q1A2", "Q1A3"))
        )
        val surveyJson = buildSurveyJson("Q1", "MCQ", mapOf(
            "Q1" to "Select Hobbies",
            "Q1A1" to "Reading",
            "Q1A2" to "Sports",
            "Q1A3" to "Music"
        ))
        val labels = mapOf("Q1" to "Select Hobbies", "Q1A1" to "Reading", "Q1A2" to "Sports", "Q1A3" to "Music")

        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to listOf("A1", "A3")))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(1)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("MCQ", question.type)
        assertEquals(listOf(AnalyticsOption("A1", "Reading"), AnalyticsOption("A2", "Sports"), AnalyticsOption("A3", "Music")), question.options)
        assertNull(question.responses)
        assertEquals(
            listOf(FrequencyCount("A1", 1), FrequencyCount("A2", 0), FrequencyCount("A3", 1)),
            question.frequencyCounts
        )
    }

    @Test
    fun `getAnalytics handles TEXT question with raw values`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.String, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "TEXT", mapOf("Q1" to "Your Name"))
        val labels = mapOf("Q1" to "Your Name")

        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to "Alice")),
            buildResponse(mapOf("Q1.value" to "Bob"))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(2)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("TEXT", question.type)
        assertEquals("Your Name", question.title)
        assertNull(question.options)
        assertEquals(listOf("Alice", "Bob"), question.responses)
    }

    @Test
    fun `getAnalytics handles NUMBER question`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Int, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "NUMBER", mapOf("Q1" to "Your Age"))
        val labels = mapOf("Q1" to "Your Age")

        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to 25)),
            buildResponse(mapOf("Q1.value" to 30))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(2)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("NUMBER", question.type)
        assertNull(question.responses)
        assertNotNull(question.numberSummary)
        assertEquals(NumberSummary(min = 25.0, max = 30.0, mean = 27.5, median = 27.5, sum = 55.0, count = 2), question.numberSummary)
    }

    @Test
    fun `getAnalytics skips empty response values`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.String, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "TEXT", mapOf("Q1" to "Name"))
        val labels = mapOf("Q1" to "Name")

        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to "Alice")),
            buildResponse(mapOf("Q1.value" to "")),
            buildResponse(mapOf("Q1.value" to "  ")),
            buildResponse(emptyMap())
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(4)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions[0].responses!!.size)
        assertEquals("Alice", result.questions[0].responses!![0])
    }

    @Test
    fun `getAnalytics handles RANKING question type`() {
        val schema = listOf(
            buildResponseField("Q1A1", ColumnName.VALUE, ReturnType.Int, "Q1A1.value"),
            buildResponseField("Q1A2", ColumnName.VALUE, ReturnType.Int, "Q1A2.value"),
            buildResponseField("Q1A3", ColumnName.VALUE, ReturnType.Int, "Q1A3.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", listOf("Q1A1", "Q1A2", "Q1A3"))
        )
        val surveyJson = buildSurveyJson("Q1", "RANKING", mapOf(
            "Q1" to "Rank these items",
            "Q1A1" to "First Item",
            "Q1A2" to "Second Item",
            "Q1A3" to "Third Item"
        ))
        val labels = mapOf("Q1" to "Rank these items", "Q1A1" to "First Item", "Q1A2" to "Second Item", "Q1A3" to "Third Item")

        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf(
                "Q1A1.value" to 2,
                "Q1A2.value" to 1,
                "Q1A3.value" to 3
            ))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(1)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("RANKING", question.type)
        assertNull(question.responses)
        assertNotNull(question.rankingSummary)
        // Response: A1=rank2, A2=rank1, A3=rank3
        assertEquals(
            listOf(
                RankingSummaryItem("A1", 2.0, 1),
                RankingSummaryItem("A2", 1.0, 1),
                RankingSummaryItem("A3", 3.0, 1)
            ),
            question.rankingSummary
        )
    }

    @Test
    fun `getAnalytics handles SIGNATURE as presence-only type`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.File, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "SIGNATURE", mapOf("Q1" to "Sign Here"))
        val labels = mapOf("Q1" to "Sign Here")

        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to "some-file-data"))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(1)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("SIGNATURE", question.type)
        assertNull(question.responses)
        assertEquals(PresenceCount(presentCount = 1, totalResponses = 1), question.presenceCount)
    }

    @Test
    fun `getAnalytics skips questions with no type info`() {
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )

        val processed = buildProcessedSurvey(emptyList(), componentIndices)

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns emptyList()
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(0)

        val result = analyticsService.getAnalytics(surveyId)

        assertTrue(result.questions.isEmpty())
    }

    @Test
    fun `getAnalytics infers type from ReturnType when question type not in survey JSON`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.String, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )

        val processed = buildProcessedSurvey(schema, componentIndices)
        val responses = listOf(buildResponse(mapOf("Q1.value" to "test")))

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(1)

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        assertEquals("TEXT", result.questions[0].type)
    }

    @Test
    fun `getAnalytics aggregates SCQ frequency counts with multiple responses`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Enum(setOf("A1", "A2", "A3")), "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", listOf("Q1A1", "Q1A2", "Q1A3"))
        )
        val surveyJson = buildSurveyJson("Q1", "SCQ", mapOf(
            "Q1" to "Color", "Q1A1" to "Red", "Q1A2" to "Blue", "Q1A3" to "Green"
        ))
        val labels = mapOf("Q1" to "Color", "Q1A1" to "Red", "Q1A2" to "Blue", "Q1A3" to "Green")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to "A1")),
            buildResponse(mapOf("Q1.value" to "A1")),
            buildResponse(mapOf("Q1.value" to "A2")),
            buildResponse(mapOf("Q1.value" to "A1")),
            buildResponse(mapOf("Q1.value" to "A3"))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(5)

        val result = analyticsService.getAnalytics(surveyId)
        val question = result.questions[0]

        assertNull(question.responses)
        assertEquals(
            listOf(FrequencyCount("A1", 3), FrequencyCount("A2", 1), FrequencyCount("A3", 1)),
            question.frequencyCounts
        )
    }

    @Test
    fun `getAnalytics aggregates MCQ frequency counts with overlapping selections`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.List(setOf("A1", "A2", "A3")), "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", listOf("Q1A1", "Q1A2", "Q1A3"))
        )
        val surveyJson = buildSurveyJson("Q1", "MCQ", mapOf(
            "Q1" to "Hobbies", "Q1A1" to "A", "Q1A2" to "B", "Q1A3" to "C"
        ))
        val labels = mapOf("Q1" to "Hobbies", "Q1A1" to "A", "Q1A2" to "B", "Q1A3" to "C")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to listOf("A1", "A2"))),
            buildResponse(mapOf("Q1.value" to listOf("A2", "A3"))),
            buildResponse(mapOf("Q1.value" to listOf("A3")))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(3)

        val result = analyticsService.getAnalytics(surveyId)
        val question = result.questions[0]

        assertEquals(
            listOf(FrequencyCount("A1", 1), FrequencyCount("A2", 2), FrequencyCount("A3", 2)),
            question.frequencyCounts
        )
    }

    @Test
    fun `getAnalytics computes NPS summary correctly`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Int, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "NPS", mapOf("Q1" to "Recommend?"))
        val labels = mapOf("Q1" to "Recommend?")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        // 3 detractors (5,3,6), 1 passive (8), 2 promoters (9,10)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to 5)),
            buildResponse(mapOf("Q1.value" to 3)),
            buildResponse(mapOf("Q1.value" to 8)),
            buildResponse(mapOf("Q1.value" to 9)),
            buildResponse(mapOf("Q1.value" to 10)),
            buildResponse(mapOf("Q1.value" to 6))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(6)

        val result = analyticsService.getAnalytics(surveyId)
        val question = result.questions[0]

        assertNull(question.responses)
        assertNotNull(question.npsSummary)
        val nps = question.npsSummary!!
        assertEquals(3, nps.detractors)
        assertEquals(1, nps.passives)
        assertEquals(2, nps.promoters)
        assertEquals(6, nps.answeredCount)
        // score = (2 - 3) / 6 * 100 = -16.666...
        assertEquals(-16.67, nps.score, 0.01)
    }

    @Test
    fun `getAnalytics computes NPS score all promoters`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Int, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "NPS", mapOf("Q1" to "NPS"))
        val labels = mapOf("Q1" to "NPS")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to 9)),
            buildResponse(mapOf("Q1.value" to 10))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(2)

        val nps = analyticsService.getAnalytics(surveyId).questions[0].npsSummary!!
        assertEquals(0, nps.detractors)
        assertEquals(0, nps.passives)
        assertEquals(2, nps.promoters)
        assertEquals(100.0, nps.score, 0.01)
    }

    @Test
    fun `getAnalytics computes NPS score all detractors`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Int, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "NPS", mapOf("Q1" to "NPS"))
        val labels = mapOf("Q1" to "NPS")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to 0)),
            buildResponse(mapOf("Q1.value" to 6))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(2)

        val nps = analyticsService.getAnalytics(surveyId).questions[0].npsSummary!!
        assertEquals(2, nps.detractors)
        assertEquals(0, nps.passives)
        assertEquals(0, nps.promoters)
        assertEquals(-100.0, nps.score, 0.01)
    }

    @Test
    fun `getAnalytics computes number summary with odd count for median`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Int, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "NUMBER", mapOf("Q1" to "Age"))
        val labels = mapOf("Q1" to "Age")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to 10)),
            buildResponse(mapOf("Q1.value" to 20)),
            buildResponse(mapOf("Q1.value" to 30))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(3)

        val summary = analyticsService.getAnalytics(surveyId).questions[0].numberSummary!!
        assertEquals(10.0, summary.min, 0.01)
        assertEquals(30.0, summary.max, 0.01)
        assertEquals(20.0, summary.mean, 0.01)
        assertEquals(20.0, summary.median, 0.01)
        assertEquals(60.0, summary.sum, 0.01)
        assertEquals(3, summary.count)
    }

    @Test
    fun `getAnalytics returns null numberSummary when no numeric responses`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Int, "Q1.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )
        val surveyJson = buildSurveyJson("Q1", "NUMBER", mapOf("Q1" to "Age"))
        val labels = mapOf("Q1" to "Age")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns emptyList()
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(0)

        val question = analyticsService.getAnalytics(surveyId).questions[0]
        assertNull(question.numberSummary)
        assertNull(question.responses)
    }

    @Test
    fun `getAnalytics aggregates ranking with multiple responses`() {
        val schema = listOf(
            buildResponseField("Q1A1", ColumnName.VALUE, ReturnType.Int, "Q1A1.value"),
            buildResponseField("Q1A2", ColumnName.VALUE, ReturnType.Int, "Q1A2.value"),
            buildResponseField("Q1A3", ColumnName.VALUE, ReturnType.Int, "Q1A3.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", listOf("Q1A1", "Q1A2", "Q1A3"))
        )
        val surveyJson = buildSurveyJson("Q1", "RANKING", mapOf(
            "Q1" to "Rank", "Q1A1" to "X", "Q1A2" to "Y", "Q1A3" to "Z"
        ))
        val labels = mapOf("Q1" to "Rank", "Q1A1" to "X", "Q1A2" to "Y", "Q1A3" to "Z")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        // Response 1: A2=rank1, A1=rank2, A3=rank3 → sorted by rank: [A2, A1, A3]
        // Response 2: A1=rank1, A3=rank2, A2=rank3 → sorted by rank: [A1, A3, A2]
        val responses = listOf(
            buildResponse(mapOf("Q1A1.value" to 2, "Q1A2.value" to 1, "Q1A3.value" to 3)),
            buildResponse(mapOf("Q1A1.value" to 1, "Q1A2.value" to 3, "Q1A3.value" to 2))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(2)

        val ranking = analyticsService.getAnalytics(surveyId).questions[0].rankingSummary!!
        // A1: ranks [2, 1] → avg 1.5
        assertEquals(1.5, ranking.find { it.code == "A1" }!!.averageRank, 0.01)
        // A2: ranks [1, 3] → avg 2.0
        assertEquals(2.0, ranking.find { it.code == "A2" }!!.averageRank, 0.01)
        // A3: ranks [3, 2] → avg 2.5
        assertEquals(2.5, ranking.find { it.code == "A3" }!!.averageRank, 0.01)
        assertEquals(2, ranking[0].responseCount)
    }

    @Test
    fun `getAnalytics aggregates matrix SCQ_ARRAY correctly`() {
        val schema = listOf(
            buildResponseField("Q1A1", ColumnName.VALUE, ReturnType.Enum(setOf("Ac1", "Ac2")), "Q1A1.value"),
            buildResponseField("Q1A2", ColumnName.VALUE, ReturnType.Enum(setOf("Ac1", "Ac2")), "Q1A2.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", listOf("Q1A1", "Q1A2", "Q1Ac1", "Q1Ac2"))
        )
        val surveyJson = buildSurveyJson("Q1", "SCQ_ARRAY", mapOf(
            "Q1" to "Matrix", "Q1A1" to "Row1", "Q1A2" to "Row2", "Q1Ac1" to "Col1", "Q1Ac2" to "Col2"
        ))
        val labels = mapOf("Q1" to "Matrix", "Q1A1" to "Row1", "Q1A2" to "Row2", "Q1Ac1" to "Col1", "Q1Ac2" to "Col2")
        val processed = buildProcessedSurvey(schema, componentIndices, surveyJson, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1A1.value" to "Ac1", "Q1A2.value" to "Ac2")),
            buildResponse(mapOf("Q1A1.value" to "Ac1", "Q1A2.value" to "Ac1"))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(2)

        val question = analyticsService.getAnalytics(surveyId).questions[0]
        assertNull(question.responses)
        assertNotNull(question.matrixSummary)
        val matrix = question.matrixSummary!!
        // A1->Ac1: 2, A1->Ac2: 0, A2->Ac1: 1, A2->Ac2: 1
        assertEquals(2, matrix.find { it.rowCode == "A1" && it.columnCode == "Ac1" }!!.count)
        assertEquals(0, matrix.find { it.rowCode == "A1" && it.columnCode == "Ac2" }!!.count)
        assertEquals(1, matrix.find { it.rowCode == "A2" && it.columnCode == "Ac1" }!!.count)
        assertEquals(1, matrix.find { it.rowCode == "A2" && it.columnCode == "Ac2" }!!.count)
    }

    @Test
    fun `getAnalytics returns null responses for aggregated types and raw responses for text types`() {
        val schema = listOf(
            buildResponseField("Q1", ColumnName.VALUE, ReturnType.Enum(setOf("A1")), "Q1.value"),
            buildResponseField("Q2", ColumnName.VALUE, ReturnType.String, "Q2.value")
        )
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1", "Q2")),
            buildComponentIndex("Q1", listOf("Q1A1")),
            buildComponentIndex("Q2", emptyList())
        )
        val factory = JsonNodeFactory.instance
        val root = factory.objectNode()
        val defaultLang = factory.objectNode()
        defaultLang.put("code", "en")
        defaultLang.put("name", "English")
        root.set<ObjectNode>("defaultLang", defaultLang)

        val q1Node = factory.objectNode()
        q1Node.put("code", "Q1")
        q1Node.put("type", "SCQ")
        val q1Content = factory.objectNode()
        val q1En = factory.objectNode()
        q1En.put("label", "Q1")
        q1Content.set<ObjectNode>("en", q1En)
        q1Node.set<ObjectNode>("content", q1Content)
        q1Node.set<ObjectNode>("answers", factory.arrayNode().add(factory.objectNode().also { it.put("code", "A1"); val c = factory.objectNode(); val e = factory.objectNode(); e.put("label", "Opt1"); c.set<ObjectNode>("en", e); it.set<ObjectNode>("content", c) }))

        val q2Node = factory.objectNode()
        q2Node.put("code", "Q2")
        q2Node.put("type", "TEXT")
        val q2Content = factory.objectNode()
        val q2En = factory.objectNode()
        q2En.put("label", "Q2")
        q2Content.set<ObjectNode>("en", q2En)
        q2Node.set<ObjectNode>("content", q2Content)
        q2Node.set<ObjectNode>("answers", factory.arrayNode())

        val groupNode = factory.objectNode()
        groupNode.put("code", "G1")
        val questionsArray = factory.arrayNode()
        questionsArray.add(q1Node)
        questionsArray.add(q2Node)
        groupNode.set<ObjectNode>("questions", questionsArray)
        root.set<ObjectNode>("groups", factory.arrayNode().add(groupNode))

        val labels = mapOf("Q1" to "Q1", "Q1A1" to "Opt1", "Q2" to "Q2")
        val processed = buildProcessedSurvey(schema, componentIndices, root, labels)
        val responses = listOf(
            buildResponse(mapOf("Q1.value" to "A1", "Q2.value" to "hello"))
        )

        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns responses
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(1)

        val result = analyticsService.getAnalytics(surveyId)
        val scqQuestion = result.questions.find { it.type == "SCQ" }!!
        val textQuestion = result.questions.find { it.type == "TEXT" }!!

        // SCQ: has frequencyCounts, no raw responses
        assertNotNull(scqQuestion.frequencyCounts)
        assertNull(scqQuestion.responses)

        // TEXT: has raw responses, no summary fields
        assertNotNull(textQuestion.responses)
        assertNull(textQuestion.frequencyCounts)
        assertNull(textQuestion.numberSummary)
        assertNull(textQuestion.npsSummary)
    }

    @Test
    fun `getAnalytics respects maxResponses parameter via limit`() {
        val processed = buildProcessedSurvey(emptyList(), emptyList())
        every { designService.getProcessedSurvey(surveyId, false) } returns processed
        every { responseRepository.findCompletedValuesBySurveyId(surveyId, any()) } returns emptyList()
        every { responseRepository.analyticsResponseCounts(surveyId) } returns mockCounts(0)

        analyticsService.getAnalytics(surveyId, 100)

        verify {
            responseRepository.findCompletedValuesBySurveyId(surveyId, 100)
        }
    }

    // --- Helper methods ---

    private fun buildProcessedSurvey(
        schema: List<ResponseField>,
        componentIndices: List<ComponentIndex>,
        surveyJson: ObjectNode = buildEmptySurveyJson(),
        labels: Map<String, String> = emptyMap()
    ): ProcessedSurvey {
        val survey = buildSurveyEntity()
        val version = buildVersionEntity()
        val validationOutput = mockk<ValidationJsonOutput>()
        every { validationOutput.survey } returns surveyJson
        every { validationOutput.schema } returns schema
        every { validationOutput.componentIndexList } returns componentIndices
        every { validationOutput.defaultSurveyLang() } returns SurveyLang("en", "English")
        every { validationOutput.labels() } returns labels
        return ProcessedSurvey(survey, version, validationOutput)
    }

    private fun buildEmptySurveyJson(): ObjectNode {
        val factory = JsonNodeFactory.instance
        val root = factory.objectNode()
        val defaultLang = factory.objectNode()
        defaultLang.put("code", "en")
        defaultLang.put("name", "English")
        root.set<ObjectNode>("defaultLang", defaultLang)
        root.set<ObjectNode>("groups", factory.arrayNode())
        return root
    }

    private fun buildSurveyEntity() = SurveyEntity(
        id = surveyId,
        creationDate = now,
        lastModified = now,
        name = "Test Survey",
        status = Status.ACTIVE,
        usage = Usage.MIXED,
        quota = -1,
        canLockSurvey = true,
        startDate = null,
        endDate = null,
        description = null,
        image = null,
        navigationData = SurveyNavigationData(),
        saveIp = false,
        saveTimings = false,
        backgroundAudio = false,
        recordGps = false
    )

    private fun buildVersionEntity() = VersionEntity(
        version = 1,
        surveyId = surveyId,
        subVersion = 0,
        valid = true,
        published = true,
        schema = emptyList(),
        lastModified = now
    )

    private fun buildResponse(values: Map<String, Any>): String {
        return com.qlarr.backend.configurations.objectMapper.writeValueAsString(values)
    }

    private fun mockCounts(completed: Int, incomplete: Int = 0, preview: Int = 0): AnalyticsResponseCount {
        val counts = mockk<AnalyticsResponseCount>()
        every { counts.completedCount } returns completed
        every { counts.incompleteCount } returns incomplete
        every { counts.previewCount } returns preview
        return counts
    }

    private fun buildResponseField(
        componentCode: String,
        columnName: ColumnName,
        dataType: ReturnType,
        valueKey: String
    ): ResponseField {
        val field = mockk<ResponseField>()
        every { field.componentCode } returns componentCode
        every { field.columnName } returns columnName
        every { field.dataType } returns dataType
        every { field.toValueKey() } returns valueKey
        return field
    }

    private fun buildComponentIndex(
        code: String,
        children: List<String>
    ): ComponentIndex {
        val index = mockk<ComponentIndex>()
        every { index.code } returns code
        every { index.children } returns children
        return index
    }

    private fun buildSurveyJson(
        questionCode: String,
        questionType: String,
        labels: Map<String, String>
    ): ObjectNode {
        val factory = JsonNodeFactory.instance
        val root = factory.objectNode()

        val questionNode = factory.objectNode()
        questionNode.put("code", questionCode)
        questionNode.put("type", questionType)

        val content = factory.objectNode()
        val enContent = factory.objectNode()
        enContent.put("label", labels[questionCode] ?: questionCode)
        content.set<ObjectNode>("en", enContent)
        questionNode.set<ObjectNode>("content", content)

        val answerNodes = factory.arrayNode()
        labels.filter { it.key.startsWith(questionCode + "A") }.forEach { (code, label) ->
            val answerNode = factory.objectNode()
            answerNode.put("code", code.removePrefix(questionCode))
            val answerContent = factory.objectNode()
            val answerEnContent = factory.objectNode()
            answerEnContent.put("label", label)
            answerContent.set<ObjectNode>("en", answerEnContent)
            answerNode.set<ObjectNode>("content", answerContent)
            answerNodes.add(answerNode)
        }
        questionNode.set<ObjectNode>("answers", answerNodes)

        val groupNode = factory.objectNode()
        groupNode.put("code", "G1")
        val questionsArray = factory.arrayNode()
        questionsArray.add(questionNode)
        groupNode.set<ObjectNode>("questions", questionsArray)

        val defaultLang = factory.objectNode()
        defaultLang.put("code", "en")
        defaultLang.put("name", "English")
        root.set<ObjectNode>("defaultLang", defaultLang)

        val groupsArray = factory.arrayNode()
        groupsArray.add(groupNode)
        root.set<ObjectNode>("groups", groupsArray)

        return root
    }
}
