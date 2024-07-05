package com.frankie.backend.services

import com.frankie.backend.api.survey.EditSurveyRequest
import com.frankie.backend.api.survey.Status
import com.frankie.backend.api.survey.SurveyCreateRequest
import com.frankie.backend.api.survey.SurveyDTO
import com.frankie.backend.common.isValidName
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.*
import com.frankie.backend.helpers.S3Helper
import com.frankie.backend.mappers.SurveyMapper
import com.frankie.backend.persistence.entities.SurveyEntity
import com.frankie.backend.persistence.entities.VersionEntity
import com.frankie.backend.persistence.repositories.ResponseRepository
import com.frankie.backend.persistence.repositories.SurveyRepository
import com.frankie.backend.persistence.repositories.VersionRepository
import com.frankie.expressionmanager.usecase.ValidationJsonOutput
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SurveyService(
        private val surveyMapper: SurveyMapper,
        private val surveyRepository: SurveyRepository,
        private val versionRepository: VersionRepository,
        private val responsesRepository: ResponseRepository,
        private val designService: DesignService,
        private val s3Helper: S3Helper,

        ) {
    @Transactional(rollbackFor = [DuplicateSurveyException::class])
    fun create(surveyCreateRequest: SurveyCreateRequest): SurveyDTO {
        if (!surveyCreateRequest.name.isValidName()) {
            throw InvalidSurveyName()
        }
        val surveyEntity = surveyMapper.mapCreateRequestToEntity(surveyCreateRequest)
        if (surveyEntity.startDate != null
                && surveyEntity.endDate != null && surveyEntity.startDate.isAfter(surveyEntity.endDate)
        ) {
            throw InvalidSurveyDates()
        }
        try {
            val saved = surveyRepository.save(surveyEntity)
            versionRepository.save(
                    VersionEntity(
                            version = 1,
                            subVersion = 1,
                            surveyId = saved.id!!,
                            valid = false,
                            published = false,
                            schema = listOf(),
                            lastModified = nowUtc()
                    )
            )
            designService.setDesign(
                    surveyId = surveyEntity.id!!,
                    design = ValidationJsonOutput.new().survey,
                    version = 1,
                    subVersion = 1,
                    sampleSurvey = true
            )
        } catch (exception: DataIntegrityViolationException) {
            // we assume here that at least only the email constraint could be violated
            throw DuplicateSurveyException()
        }
        return surveyMapper.mapEntityToDto(surveyEntity)
    }

    fun getSurveyById(surveyId: UUID, bypassPermission: Boolean = false): SurveyDTO {
        return surveyRepository.findByIdOrNull(surveyId)?.let {
            surveyMapper.mapEntityToDto(it)
        } ?: throw SurveyNotFoundException()
    }

    fun edit(surveyId: UUID, editSurveyRequest: EditSurveyRequest): SurveyDTO {
        if (editSurveyRequest.name?.isValidName() == false) {
            throw InvalidSurveyName()
        }
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        if (survey.status == Status.CLOSED) {
            throw SurveyIsClosedException()
        }
        val newSurvey = SurveyEntity(
                // those 3 fields we cannot change
                id = survey.id,
                status = survey.status,
                creationDate = survey.creationDate,
                lastModified = nowUtc(),
                // everything else is editable
                name = editSurveyRequest.name?.trim() ?: survey.name,
                usage = editSurveyRequest.usage ?: survey.usage,
                endDate = editSurveyRequest.endDate,
                startDate = editSurveyRequest.startDate,
                quota = editSurveyRequest.quota ?: survey.quota,
                canLockSurvey = editSurveyRequest.canLockSurvey ?: survey.canLockSurvey,
        )
        if (newSurvey.startDate != null
                && newSurvey.endDate != null && newSurvey.startDate.isAfter(newSurvey.endDate)
        ) {
            throw InvalidSurveyDates()
        }
        try {
            return surveyMapper.mapEntityToDto(surveyRepository.save(newSurvey))
        } catch (exception: DataIntegrityViolationException) {
            // we assume here that at least only the email constraint could be violated
            throw DuplicateSurveyException()
        }
    }

    @Transactional
    fun delete(surveyId: UUID) {
        surveyRepository.findByIdOrNull(surveyId)?.let {
            if (Status.ACTIVE == it.status) {
                throw SurveyIsActiveException()
            }
            responsesRepository.deleteBySurveyId(surveyId)
            versionRepository.deleteBySurveyId(surveyId)
            surveyRepository.delete(it)
            s3Helper.deleteSurveyFiles(surveyId)
        } ?: throw SurveyNotFoundException()
    }

    fun clone(surveyId: UUID, name: String): SurveyDTO {
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val cloned = survey.copy(
                id = null,
                name = name,
                status = Status.DRAFT,
                creationDate = nowUtc(),
                lastModified = nowUtc()
        )
        try {
            surveyRepository.save(cloned)
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateSurveyException()
        }
        s3Helper.cloneResources(surveyId, cloned.id!!)
        copyDesign(surveyId, cloned.id)
        return surveyMapper.mapEntityToDto(cloned)
    }

    @Transactional
    fun copyDesign(source: UUID, destination: UUID) {
        val latestVersion = versionRepository.findLatestVersion(source)
                ?: return
        versionRepository.save(
                latestVersion.copy(
                        surveyId = destination,
                        version = 1,
                        published = false,
                        subVersion = 1,
                        lastModified = nowUtc()
                )
        )
        s3Helper.copyDesign(source, destination, latestVersion.version.toString(), "1")

    }

    fun close(surveyId: UUID): SurveyDTO {

        val entity: SurveyEntity = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        if (entity.status != Status.ACTIVE) {
            throw SurveyIsNotActiveException()
        }
        return surveyMapper.mapEntityToDto(
                surveyRepository.save(
                        entity.copy(
                                status = Status.CLOSED,
                                lastModified = nowUtc()
                        )
                )
        )
    }
}
