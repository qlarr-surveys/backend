package com.qlarr.backend.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.qlarr.backend.api.survey.*
import com.qlarr.backend.api.surveyengine.ValidationJsonOutput
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.isValidName
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.backend.exceptions.*
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.mappers.SurveyMapper
import com.qlarr.backend.persistence.entities.AutoCompleteEntity
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.backend.persistence.entities.SurveyNavigationData
import com.qlarr.backend.persistence.entities.SurveyResponseCount
import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.repositories.AutoCompleteRepository
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.backend.persistence.repositories.SurveyRepository
import com.qlarr.backend.persistence.repositories.VersionRepository
import com.qlarr.surveyengine.usecase.ValidationUseCaseWrapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*

@Service
class SurveyService(
    private val surveyMapper: SurveyMapper,
    private val surveyRepository: SurveyRepository,
    private val versionRepository: VersionRepository,
    private val responsesRepository: ResponseRepository,
    private val designService: DesignService,
    private val fileSystemHelper: FileHelper,
    private val autoCompleteRepository: AutoCompleteRepository
) {
    @Transactional(rollbackFor = [DuplicateSurveyException::class])
    fun create(surveyCreateRequest: SurveyCreateRequest): SurveyDTO {
        if (!surveyCreateRequest.name.isValidName()) {
            throw InvalidSurveyName()
        }
        val surveyEntity = surveyMapper.mapCreateRequestToEntity(surveyCreateRequest).let {
            it.copy(name = uniqueSurveyName(it.name))
        }
        if (surveyEntity.startDate != null && surveyEntity.endDate != null && surveyEntity.startDate.isAfter(
                surveyEntity.endDate
            )
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
                design = objectMapper.readTree(ValidationUseCaseWrapper.new(surveyCreateRequest.name)) as ObjectNode,
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

    fun getSurveyById(surveyId: UUID): SurveyDTO {
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
            description = editSurveyRequest.description ?: survey.description,
            image = editSurveyRequest.image ?: survey.image,
            navigationData = SurveyNavigationData(
                navigationMode = editSurveyRequest.navigationMode ?: survey.navigationData.navigationMode,
                allowPrevious = editSurveyRequest.allowPrevious ?: survey.navigationData.allowPrevious,
                resumeExpiryMillis = editSurveyRequest.resumeExpiryMillis ?: survey.navigationData.resumeExpiryMillis,
                skipInvalid = editSurveyRequest.skipInvalid ?: survey.navigationData.skipInvalid,
                allowIncomplete = editSurveyRequest.allowIncomplete ?: survey.navigationData.allowIncomplete,
                allowJump = editSurveyRequest.allowJump ?: survey.navigationData.allowJump
            )
        )
        if (newSurvey.startDate != null && newSurvey.endDate != null && newSurvey.startDate.isAfter(newSurvey.endDate)) {
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
            fileSystemHelper.deleteSurveyFiles(surveyId)
        } ?: throw SurveyNotFoundException()
    }

    @Transactional
    fun clone(surveyId: UUID): SurveyDTO {
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val cloned = survey.copy(
            id = null,
            name = uniqueSurveyName(survey.name),
            status = Status.DRAFT,
            creationDate = nowUtc(),
            lastModified = nowUtc()
        )
        try {
            surveyRepository.save(cloned)
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateSurveyException()
        }
        fileSystemHelper.cloneResources(surveyId, cloned.id!!)
        copyDesign(surveyId, cloned.id)
        autoCompleteRepository.copyAutoCompleteEntries(surveyId, cloned.id)

        return surveyMapper.mapEntityToDto(cloned)
    }

    @Transactional
    fun copyDesign(source: UUID, destination: UUID) {
        val latestVersion = versionRepository.findLatestVersion(source) ?: return
        versionRepository.save(
            latestVersion.copy(
                surveyId = destination, version = 1, published = false, subVersion = 1, lastModified = nowUtc()
            )
        )
        fileSystemHelper.copyDesign(source, destination, latestVersion.version.toString(), "1")

    }

    fun close(surveyId: UUID): SurveyDTO {

        val entity: SurveyEntity = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        if (entity.status != Status.ACTIVE) {
            throw SurveyIsNotActiveException()
        }
        return surveyMapper.mapEntityToDto(
            surveyRepository.save(
                entity.copy(
                    status = Status.CLOSED, lastModified = nowUtc()
                )
            )
        )
    }

    fun exportSurvey(surveyId: UUID): ByteArray {
        val latestVersion = versionRepository.findLatestVersion(surveyId) ?: throw SurveyNotFoundException()

        val surveyDataJson = getSurveyDataJson(surveyId)

        return fileSystemHelper.exportSurvey(surveyId, latestVersion.version.toString(), surveyDataJson)
    }

    fun getSurveyDataJson(surveyId: UUID): String {
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val latestVersion = versionRepository.findLatestVersion(surveyId)

        val surveyData = object : SurveyResponseCount {
            override val survey = survey
            override val responseCount = 0L
            override val completeResponseCount = 0L
            override val latestVersion: VersionEntity =
                latestVersion!!.copy(version = 1, subVersion = 1, published = false)
        }.let {
            val surveyDto = surveyMapper.mapEntityToSimpleResponse(it)
            val autoCompleteList = autoCompleteRepository.findBySurveyId(surveyId).mapNotNull { entity ->
                if (entity.filename.isNotBlank()) {
                    ExportedAutoCompleteResource(entity.componentId, entity.filename)
                } else null
            }
            ExportedSimpleSurvey(surveyDto, autoCompleteList)
        }.let {
            objectMapper.writeValueAsString(it)
        }

        return surveyData
    }

    fun importSurvey(inputStream: InputStream): SurveyDTO {
        var surveyDTO: SurveyDTO? = null
        var designSaved = false
        var exportSurvey: ExportedSimpleSurvey? = null

        fileSystemHelper.importSurvey(inputStream, onSurveyData = {
            exportSurvey = objectMapper.readValue(it, ExportedSimpleSurvey::class.java)
            surveyDTO = saveSurveyData(exportSurvey.survey)
            surveyDTO
        }, onDesign = {
            designSaved = true
        })

        if (!designSaved) {
            throw DesignNotAvailableException()
        }
        if (surveyDTO == null) {
            throw SurveyDefNotAvailableException()
        }

        if(exportSurvey != null) {
            saveAutoComplete(surveyDTO.id, exportSurvey.autoCompleteResources)
        }

        return surveyDTO
    }

    fun saveAutoComplete(surveyId: UUID, autoCompleteResources: List<ExportedAutoCompleteResource>) {
        autoCompleteResources.forEach { resource ->
            try {
                (objectMapper.readTree(
                    fileSystemHelper.getText(surveyId, SurveyFolder.Resources, resource.filename)
                ).takeIf { it.isArray } as? ArrayNode)?.let { arrayNode ->
                    autoCompleteRepository.save(
                        AutoCompleteEntity(
                            id = null,
                            filename = resource.filename,
                            surveyId = surveyId,
                            componentId = resource.code,
                            values = arrayNode
                        )
                    )
                }
            } catch (e: Exception) {
            }
        }
    }


    private fun uniqueSurveyName(surveyName: String): String {
        val existingNames = surveyRepository.findAllSurveyNames()

        // If the original name doesn't exist, return it as-is
        if (!existingNames.contains(surveyName)) {
            return surveyName
        }

        // Extract base name and starting number
        val regex = Regex("""^(.+)\((\d+)\)$""")
        val matchResult = regex.find(surveyName)

        val (baseName, startingNumber) = if (matchResult != null) {
            // Input already has a number suffix like "name(5)"
            val baseName = matchResult.groupValues[1]
            val number = matchResult.groupValues[2].toInt()
            Pair(baseName, number)
        } else {
            // Input has no number suffix, start from 1
            Pair(surveyName, 1)
        }

        // Find the first available number
        var increment = startingNumber
        var candidateName: String

        do {
            candidateName = "$baseName($increment)"
            increment++
        } while (existingNames.contains(candidateName))

        return candidateName
    }

    fun saveSurveyData(simpleSurveyDto: SimpleSurveyDto): SurveyDTO {
        val savedSurvey = try {
            surveyRepository.save(
                SurveyEntity(
                    creationDate = nowUtc(),
                    lastModified = nowUtc(),
                    name = uniqueSurveyName(simpleSurveyDto.name),
                    status = Status.DRAFT,
                    startDate = simpleSurveyDto.startDate,
                    endDate = simpleSurveyDto.endDate,
                    usage = simpleSurveyDto.usage,
                    quota = -1,
                    canLockSurvey = false,
                    image = simpleSurveyDto.image,
                    description = simpleSurveyDto.description,
                    navigationData = simpleSurveyDto.navigationData,
                )
            )
        } catch (exception: DataIntegrityViolationException) {
            // we assume here that at least only the email constraint could be violated
            throw DuplicateSurveyException()
        }


        versionRepository.save(
            VersionEntity(
                version = 1,
                surveyId = savedSurvey.id!!,
                subVersion = 1,
                valid = simpleSurveyDto.latestVersion.valid,
                published = false,
                schema = listOf(),
                lastModified = nowUtc()
            )
        )
        return surveyMapper.mapEntityToDto(savedSurvey)
    }
}
