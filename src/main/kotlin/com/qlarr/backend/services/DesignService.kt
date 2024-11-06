package com.qlarr.backend.services

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qlarr.backend.api.design.DesignDto
import com.qlarr.backend.api.offline.DesignDiffDto
import com.qlarr.backend.api.offline.PublishInfo
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.api.version.VersionDto
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.exceptions.*
import com.qlarr.backend.expressionmanager.SurveyProcessor
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.mappers.VersionMapper
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.backend.persistence.repositories.SurveyRepository
import com.qlarr.backend.persistence.repositories.VersionRepository
import com.qlarr.surveyengine.ext.resources
import com.qlarr.surveyengine.model.jacksonKtMapper
import com.qlarr.surveyengine.usecase.ValidationJsonOutput
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class DesignService(
    private val versionRepository: VersionRepository,
    private val surveyRepository: SurveyRepository,
    private val helper: FileHelper,
    private val versionMapper: VersionMapper,
    private val responseRepository: ResponseRepository,
) {

    @Transactional
    fun setDesign(
        surveyId: UUID,
        design: ObjectNode,
        version: Int,
        subVersion: Int,
        sampleSurvey: Boolean = false,
    ): DesignDto {
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        if (survey.status == Status.CLOSED) {
            throw SurveyIsClosedException()
        }
        val latestVersion = versionRepository.findLatestVersion(surveyId) ?: throw DesignException()
        val latestPublishedVersion = versionRepository.findLatestPublishedVersion(surveyId)
        if (version != latestVersion.version || subVersion != latestVersion.subVersion) {
            throw DesignOutOfSyncException(latestVersion.subVersion)
        }
        val versionToSave = if (latestVersion.published) latestVersion.version + 1 else latestVersion.version
        val subversionToSave = if (latestVersion.published) 1 else latestVersion.subVersion + 1

        val validationJsonOutput =
            if (sampleSurvey) SurveyProcessor.processSample(design) else SurveyProcessor.process(design)
        if (latestPublishedVersion != null) {
            val oldJson = helper.getText(surveyId, SurveyFolder.DESIGN, latestPublishedVersion.version.toString())
            val oldCodes =
                jacksonKtMapper.readValue(oldJson, ValidationJsonOutput::class.java)
                    .toDesignerInput().state.fieldNames().asSequence().toList()
            val newCodes = validationJsonOutput.toDesignerInput().state.fieldNames().asSequence().toList()
            if (!newCodes.containsAll(oldCodes)) {
                throw ComponentDeletedException(oldCodes.filter { !newCodes.contains(it) })
            }
        }
        helper.upload(
            surveyId,
            SurveyFolder.DESIGN,
            jacksonObjectMapper().writeValueAsString(validationJsonOutput),
            versionToSave.toString()
        )
        val designerInput = validationJsonOutput.toDesignerInput()
        val isValid: Boolean = (validationJsonOutput.survey["errors"] as? ArrayNode)?.let { it.size() == 0 } ?: true
        val versionEntity = latestVersion.copy(
            published = false,
            version = versionToSave,
            surveyId = surveyId,
            valid = isValid,
            subVersion = subversionToSave,
            schema = validationJsonOutput.schema,
            lastModified = nowUtc()
        )
        val saved = versionRepository.save(versionEntity)
        surveyRepository.save(survey.copy(lastModified = nowUtc()))
        return DesignDto(
            designerInput = designerInput,
            versionDto = versionMapper.toDto(saved, survey.status)
        )
    }

    fun getDesign(surveyId: UUID): DesignDto {
        val processedSurvey = getProcessedSurvey(surveyId, false)
        val designerInput = processedSurvey.validationJsonOutput.toDesignerInput()
        return DesignDto(
            designerInput = designerInput,
            versionDto = versionMapper.toDto(processedSurvey.latestVersion, processedSurvey.survey.status)
        )
    }

    //This method is unsecured, not by permissions and not by authentication
    // please double-check on who accesses this one...
    fun getProcessedSurvey(surveyId: UUID, published: Boolean): ProcessedSurvey {
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val latestVersion = if (published) {
            versionRepository.findLatestPublishedVersion(surveyId) ?: throw DesignException()
        } else {
            versionRepository.findLatestVersion(surveyId) ?: throw DesignException()
        }
        val json = helper.getText(surveyId, SurveyFolder.DESIGN, latestVersion.version.toString())
        val validationJsonOutput = jacksonKtMapper.readValue(json, ValidationJsonOutput::class.java)

        return ProcessedSurvey(survey, latestVersion, validationJsonOutput)
    }

    fun getProcessedSurveyByVersion(surveyId: UUID, version: Int): ProcessedSurvey {
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val latestVersion = versionRepository.findBySurveyIdAndVersion(surveyId, version) ?: throw DesignException()
        val json = helper.getText(surveyId, SurveyFolder.DESIGN, latestVersion.version.toString())
        val validationJsonOutput = jacksonKtMapper.readValue(json, ValidationJsonOutput::class.java)
        return ProcessedSurvey(survey, latestVersion, validationJsonOutput)
    }

    @Transactional
    fun publish(surveyId: UUID, version: Int, subVersion: Int): VersionDto {
        val latestVersion = versionRepository.findLatestVersion(surveyId) ?: throw DesignException()
        val latestPublished = versionRepository.findLatestPublishedVersion(surveyId)
        if (version != latestVersion.version || subVersion != latestVersion.subVersion) {
            throw DesignOutOfSyncException(latestVersion.subVersion)
        }
        if (!latestVersion.valid) {
            throw InvalidDesignException()
        }
        if ((latestPublished == null && latestVersion.version != 1)
            || (latestPublished != null && latestVersion.version - latestPublished.version > 1)
        ) {
            throw DesignOutOfSyncException(latestVersion.subVersion)
        }
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        if (survey.status != Status.ACTIVE) {
            surveyRepository.save(
                survey.copy(
                    status = Status.ACTIVE,
                    lastModified = nowUtc()
                )
            )
        }
        // This is the first time to publish ever
        val saved = if (latestPublished == null) {
            latestVersion.copy(
                published = true,
                version = 1,
                subVersion = 1,
                lastModified = nowUtc()
            )
        } else {
            val oldJson = helper.getText(surveyId, SurveyFolder.DESIGN, latestPublished.version.toString())
            val oldComponentIndex =
                jacksonKtMapper.readValue(oldJson, ValidationJsonOutput::class.java).componentIndexList
            val newJson = helper.getText(surveyId, SurveyFolder.DESIGN, latestVersion.version.toString())
            val newComponentIndex =
                jacksonKtMapper.readValue(newJson, ValidationJsonOutput::class.java).componentIndexList
            val newCodes = newComponentIndex.map { it.code }
            val oldCodes = oldComponentIndex.map { it.code }
            if (!newCodes.containsAll(oldCodes)) {
                throw ComponentDeletedException(oldCodes.filter { !newCodes.contains(it) })
            }
            // no material changes... replace the current published version and increment subversion
            surveyRepository.save(survey.copy(lastModified = nowUtc()))
            if (oldComponentIndex == newComponentIndex) {
                responseRepository.changeVersion(surveyId, latestVersion.version, latestPublished.version)
                versionRepository.delete(latestVersion)
                helper.delete(surveyId, SurveyFolder.DESIGN, latestVersion.version.toString())
                helper.upload(surveyId, SurveyFolder.DESIGN, newJson, latestPublished.version.toString())
                latestPublished.copy(
                    published = true,
                    subVersion = latestPublished.subVersion + 1,
                    lastModified = nowUtc()
                )
            } else {
                // Material changes... we have a new published version
                latestVersion.copy(
                    published = true,
                    subVersion = 1,
                    lastModified = nowUtc()
                )
            }
        }
        versionRepository.save(saved)
        return versionMapper.toDto(saved, survey.status)
    }

    fun offlineDesignDiff(surveyId: UUID, publishInfo: PublishInfo): DesignDiffDto {
        val survey = surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        if (survey.status == Status.CLOSED) {
            throw SurveyIsClosedException()
        }
        val publishedVersion =
            versionRepository.findLatestPublishedVersion(surveyId) ?: throw NoPublishedVersionException()
        return if (!publishInfo.listFiles && publishedVersion.version == publishInfo.version && publishedVersion.subVersion == publishInfo.subVersion) {
            DesignDiffDto(publishInfo = publishInfo)
        } else {
            val json = helper.getText(surveyId, SurveyFolder.DESIGN, publishedVersion.version.toString())
            val validationJsonOutput = jacksonKtMapper.readValue(json, ValidationJsonOutput::class.java)
            val resources = validationJsonOutput.survey.resources()
            val files = helper.surveyResourcesFiles(surveyId, resources, publishInfo.lastModified)
            return DesignDiffDto(
                files,
                PublishInfo(
                    publishedVersion.version,
                    publishedVersion.subVersion,
                    publishInfo.listFiles,
                    publishedVersion.lastModified!!
                ),
                validationJsonOutput = validationJsonOutput
            )
        }
    }
}

data class ProcessedSurvey(
    val survey: SurveyEntity,
    val latestVersion: VersionEntity,
    val validationJsonOutput: ValidationJsonOutput
)