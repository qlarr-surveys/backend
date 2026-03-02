package com.qlarr.backend.services

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.api.survey.Usage
import com.qlarr.backend.api.surveyengine.ValidationJsonOutput
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.backend.persistence.entities.SurveyNavigationData
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.model.ComponentIndex
import com.qlarr.surveyengine.model.SurveyLang
import com.qlarr.surveyengine.model.exposed.ColumnName
import com.qlarr.surveyengine.model.exposed.NavigationIndex
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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
    fun `getAnalytics uses published survey version`() {
        val processed = buildProcessedSurvey(emptyList(), emptyList())
        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(emptyList())
        every { responseRepository.completedSurveyCount(surveyId) } returns 0

        analyticsService.getAnalytics(surveyId)

        verify(exactly = 1) { designService.getProcessedSurvey(surveyId, true) }
    }

    @Test
    fun `getAnalytics returns correct survey title and total responses`() {
        val processed = buildProcessedSurvey(emptyList(), emptyList())
        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(emptyList())
        every { responseRepository.completedSurveyCount(surveyId) } returns 42

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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 2

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("Q1", question.id)
        assertEquals("SCQ", question.type)
        assertEquals("Favorite Color", question.title)
        assertEquals(listOf("Red", "Blue"), question.options)
        assertEquals(2, question.responses.size)
        assertEquals("Red", question.responses[0])
        assertEquals("Blue", question.responses[1])
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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 1

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        val question = result.questions[0]
        assertEquals("MCQ", question.type)
        assertEquals(listOf("Reading", "Sports", "Music"), question.options)
        @Suppress("UNCHECKED_CAST")
        val responseValues = question.responses[0] as List<String>
        assertEquals(listOf("Reading", "Music"), responseValues)
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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 2

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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 2

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        assertEquals("NUMBER", result.questions[0].type)
        assertEquals(listOf(25, 30), result.questions[0].responses)
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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 4

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions[0].responses.size)
        assertEquals("Alice", result.questions[0].responses[0])
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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 1

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        assertEquals("RANKING", result.questions[0].type)
        @Suppress("UNCHECKED_CAST")
        val rankedItems = result.questions[0].responses[0] as List<String>
        assertEquals(listOf("Second Item", "First Item", "Third Item"), rankedItems)
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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 1

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        assertEquals("SIGNATURE", result.questions[0].type)
        assertEquals(true, result.questions[0].responses[0])
    }

    @Test
    fun `getAnalytics skips questions with no type info`() {
        val componentIndices = listOf(
            buildComponentIndex("Survey", listOf("G1")),
            buildComponentIndex("G1", listOf("Q1")),
            buildComponentIndex("Q1", emptyList())
        )

        val processed = buildProcessedSurvey(emptyList(), componentIndices)

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(emptyList())
        every { responseRepository.completedSurveyCount(surveyId) } returns 0

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

        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(responses)
        every { responseRepository.completedSurveyCount(surveyId) } returns 1

        val result = analyticsService.getAnalytics(surveyId)

        assertEquals(1, result.questions.size)
        assertEquals("TEXT", result.questions[0].type)
    }

    @Test
    fun `getAnalytics respects maxResponses parameter via pageable`() {
        val processed = buildProcessedSurvey(emptyList(), emptyList())
        every { designService.getProcessedSurvey(surveyId, true) } returns processed
        every { responseRepository.findCompletedBySurveyId(surveyId, any()) } returns PageImpl(emptyList())
        every { responseRepository.completedSurveyCount(surveyId) } returns 0

        analyticsService.getAnalytics(surveyId, 100)

        verify {
            responseRepository.findCompletedBySurveyId(surveyId, match<Pageable> { it.pageSize == 100 })
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
        navigationData = SurveyNavigationData()
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

    private fun buildResponse(values: Map<String, Any>) = SurveyResponseEntity(
        id = UUID.randomUUID(),
        surveyId = surveyId,
        version = 1,
        surveyor = null,
        navigationIndex = NavigationIndex.End(""),
        startDate = now,
        submitDate = now,
        lang = "en",
        preview = false,
        values = values
    )

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
