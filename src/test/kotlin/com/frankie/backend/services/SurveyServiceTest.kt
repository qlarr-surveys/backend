package com.frankie.backend.services

import com.frankie.backend.api.survey.EditSurveyRequest
import com.frankie.backend.api.survey.Status
import com.frankie.backend.api.survey.Usage
import com.frankie.backend.common.UserUtils
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.SurveyIsActiveException
import com.frankie.backend.exceptions.SurveyIsClosedException
import com.frankie.backend.helpers.FileHelper
import com.frankie.backend.mappers.SurveyMapper
import com.frankie.backend.mappers.VersionMapper
import com.frankie.backend.persistence.entities.SurveyEntity
import com.frankie.backend.persistence.repositories.ResponseRepository
import com.frankie.backend.persistence.repositories.SurveyRepository
import com.frankie.backend.persistence.repositories.VersionRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import jakarta.persistence.*
import org.junit.Assert.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import java.util.*

@ExtendWith(MockKExtension::class)
class SurveyServiceTest {

    private val surveyMapper = SurveyMapper(VersionMapper())

    @MockK
    private lateinit var userUtils: UserUtils

    @MockK
    private lateinit var versionRepository: VersionRepository

    @MockK
    private lateinit var designService: DesignService

    @MockK
    private lateinit var responseRepository: ResponseRepository

    @MockK
    private lateinit var fileSystemHelper: FileHelper

    @MockK
    private lateinit var surveyRepository: SurveyRepository


    @InjectMockKs
    private lateinit var surveyService: SurveyService


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `can otherwise view survey with permission`() {
        val surveyID = UUID.randomUUID()
        val userID = UUID.randomUUID()
        val survey = buildSurvey(surveyID)
        every { userUtils.currentUserId() } returns userID
        every { surveyRepository.findByIdOrNull(surveyID) } returns survey
        Assertions.assertEquals(surveyMapper.mapEntityToDto(survey), surveyService.getSurveyById(surveyID))
    }

    @Test
    fun `cannot edit if survey is closed`() {
        val surveyID = UUID.randomUUID()
        val userID = UUID.randomUUID()
        val survey = buildSurvey(surveyID, Status.CLOSED)
        every { userUtils.currentUserId() } returns userID
        every { surveyRepository.findByIdOrNull(surveyID) } returns survey
        assertThrows(SurveyIsClosedException::class.java) { surveyService.edit(surveyID, editRequest()) }
    }

    @Test
    fun `can otherwise edit survey with permission`() {
        val surveyID = UUID.randomUUID()
        val userID = UUID.randomUUID()
        val survey = buildSurvey(surveyID)
        val surveyToSave = slot<SurveyEntity>()
        val modifiedSurvey = survey.copy(lastModified = nowUtc())
        every { userUtils.currentUserId() } returns userID
        every { surveyRepository.findByIdOrNull(surveyID) } returns survey
        every { surveyRepository.save(capture(surveyToSave)) } returns modifiedSurvey
        surveyService.edit(surveyID, editRequest())
        Assertions.assertEquals(surveyID, surveyToSave.captured.id)
        Assertions.assertEquals(NEW_NAME, surveyToSave.captured.name)
    }

    @Test
    fun `cannot delete if survey is active`() {
        val surveyID = UUID.randomUUID()
        val userID = UUID.randomUUID()
        val survey = buildSurvey(surveyID, Status.ACTIVE)
        every { userUtils.currentUserId() } returns userID
        every { surveyRepository.findByIdOrNull(surveyID) } returns survey
        assertThrows(SurveyIsActiveException::class.java) { surveyService.delete(surveyID) }
    }

    @Test
    fun `can otherwise delete survey with permission`() {
        val surveyID = UUID.randomUUID()
        val userID = UUID.randomUUID()
        val survey = buildSurvey(surveyID, Status.DRAFT)
        every { userUtils.currentUserId() } returns userID
        every { surveyRepository.findByIdOrNull(surveyID) } returns survey
        justRun { surveyRepository.delete(survey) }
        justRun { versionRepository.deleteBySurveyId(surveyID) }
        justRun { responseRepository.deleteBySurveyId(surveyID) }
        justRun { fileSystemHelper.deleteSurveyFiles(surveyID) }
        surveyService.delete(surveyID)
        verify(exactly = 1) { surveyRepository.delete(survey) }
    }

    companion object {
        private const val NEW_NAME = "new Survey name"
        fun buildSurvey(id: UUID, status: Status = Status.ACTIVE) = SurveyEntity(
                id = id,
                creationDate = nowUtc(),
                lastModified = nowUtc(),
                name = "Survey",
                status = status,
                usage = Usage.MIXED,
                quota = -1,
                canLockSurvey = true,
                startDate = null,
                endDate = null,
        )
    }


    private fun editRequest() = EditSurveyRequest(name = NEW_NAME)
}
