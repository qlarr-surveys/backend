package com.qlarr.backend.services

import com.qlarr.backend.api.response.ResponseCountDto
import com.qlarr.backend.api.response.ResponseUploadFile
import com.qlarr.backend.api.response.UploadResponseRequestData
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.UserUtils
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.backend.exceptions.*
import com.qlarr.backend.expressionmanager.SurveyProcessor
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.model.exposed.NavigationDirection
import com.qlarr.surveyengine.model.exposed.NavigationIndex
import com.qlarr.surveyengine.model.exposed.SurveyMode
import com.qlarr.surveyengine.usecase.SurveyDesignWithErrorException
import org.springframework.core.io.InputStreamResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders.CONTENT_LENGTH
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class ResponseOpsService(
    private val responseRepository: ResponseRepository,
    private val surveyService: SurveyService,
    private val helper: FileHelper,
    private val userUtils: UserUtils,
    private val designService: DesignService,
) {
    fun uploadResponseFile(
        serverName: String,
        surveyId: UUID,
        responseId: UUID,
        questionId: String,
        isPreview: Boolean,
        file: MultipartFile
    ): ResponseUploadFile {
        val survey = surveyService.getSurveyById(surveyId)
        if (!isPreview && !survey.isActive()) {
            throw SurveyIsNotActiveException()
        }
        val response = responseRepository.findByIdOrNull(responseId) ?: throw ResponseNotFoundException()
        val fileName = UUID.randomUUID().toString()
        val mimeType = file.contentType
            ?: file.originalFilename?.let { Files.probeContentType(File(it).toPath()) }
            ?: "application/octet-stream"
        helper.upload(surveyId, SurveyFolder.Responses(responseId.toString()), file, mimeType, fileName)
        val responseUploadFile = ResponseUploadFile(
            file.originalFilename!!, fileName, file.size, file.contentType
                ?: ""
        )
        val newValues = response.values.toMutableMap()
            .apply { put("$questionId.value", objectMapper.convertValue(responseUploadFile, Map::class.java)) }
        responseRepository.save(response.copy(values = newValues))
        return responseUploadFile
    }

    fun uploadOfflineResponseFile(
        surveyId: UUID,
        responseId: UUID,
        filename: String,
        file: MultipartFile
    ): ResponseUploadFile {
        val survey = surveyService.getSurveyById(surveyId)
        if (survey.status != Status.ACTIVE) {
            throw SurveyIsNotActiveException()
        }
        val mimeType = file.contentType
            ?: file.originalFilename?.let { Files.probeContentType(File(it).toPath()) }
            ?: "application/octet-stream"
        helper.upload(surveyId, SurveyFolder.Responses(responseId.toString()), file, mimeType, filename)
        return ResponseUploadFile(filename, filename, file.size, file.contentType ?: "")
    }

    fun isOfflineFileAlreadyUploaded(
        surveyId: UUID,
        responseId: UUID,
        filename: String
    ): Boolean {
        return helper.doesFileExists(surveyId, SurveyFolder.Responses(responseId.toString()), filename)
    }

    fun uploadOfflineSurveyResponse(
        surveyId: UUID,
        responseId: UUID,
        uploadResponseRequestData: UploadResponseRequestData
    ): ResponseCountDto {
        val survey = designService.getProcessedSurveyByVersion(surveyId, uploadResponseRequestData.versionId)
        if (survey.survey.status != Status.ACTIVE) {
            throw SurveyIsNotActiveException()
        } else if (!survey.latestVersion.valid) {
            throw SurveyDesignWithErrorException
        } else if (responseRepository.existsById(responseId)) {
            throw ResponseAlreadySyncedException()
        }

        if (uploadResponseRequestData.navigationIndex !is NavigationIndex.End) {
            throw IncompleteResponse()
        }

        val navigationResult = SurveyProcessor.navigate(
            values = objectMapper.writeValueAsString(uploadResponseRequestData.values),
            navigationDirection = NavigationDirection.Resume,
            navigationIndex = uploadResponseRequestData.navigationIndex,
            lang = uploadResponseRequestData.lang,
            skipInvalid = false,
            processedSurvey = survey.validationJsonOutput.stringified(),
            surveyMode = SurveyMode.OFFLINE,
        )

        val isValid: Boolean =
            navigationResult.state["qlarrVariables"]?.get("Survey")?.get("validity")?.booleanValue() ?: false
        if (!isValid) {
            throw InvalidResponse()
        }

        val responseEntity = SurveyResponseEntity(
            id = responseId,
            version = uploadResponseRequestData.versionId,
            startDate = uploadResponseRequestData.startDate,
            surveyId = surveyId,
            surveyor = UUID.fromString(uploadResponseRequestData.userId),
            lang = uploadResponseRequestData.lang,
            navigationIndex = uploadResponseRequestData.navigationIndex,
            submitDate = uploadResponseRequestData.submitDate,
            values = uploadResponseRequestData.values
        )
        responseRepository.save(responseEntity)
        helper.deleteUnusedResponseFiles(surveyId, responseId, uploadResponseRequestData.values)

        return responseRepository.responseCount(
            userId = userUtils.currentUserId(),
            surveyId = surveyId
        ).let {
            ResponseCountDto(
                completeResponseCount = it.completeResponseCount.toInt(),
                userResponsesCount = it.userResponseCount.toInt()
            )
        }
    }

    fun downloadFile(
        serverName: String,
        surveyId: UUID,
        responseId: UUID,
        filename: UUID
    ): ResponseEntity<InputStreamResource> {
        val file = helper.download(surveyId, SurveyFolder.Responses(responseId.toString()), filename.toString())
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
            .header(CONTENT_TYPE, file.objectMetadata["Content-Type"]!!)
            .header(CONTENT_LENGTH, file.objectMetadata["Content-Length"]!!)
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
            .eTag(file.objectMetadata["eTag"]) // lastModified is also ava
            .body(InputStreamResource(file.inputStream))
    }

    fun downloadFileNew(
        surveyId: UUID,
        responseId: UUID,
        questionId: String
    ): ResponseEntity<InputStreamResource> {
        val response = responseRepository.findByIdOrNull(responseId) ?: throw ResponseNotFoundException()
        val questionValue = response.values["$questionId.value"]
        if (questionValue is LinkedHashMap<*, *> && questionValue.containsKey("filename")
            && questionValue.containsKey("stored_filename")
        ) {
            val file =
                helper.download(
                    surveyId,
                    SurveyFolder.Responses(responseId.toString()),
                    questionValue["stored_filename"] as String
                )
            val customFileName = "${response.surveyResponseIndex}-$questionId-${questionValue["filename"]}"
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .header(CONTENT_TYPE, file.objectMetadata["Content-Type"]!!)
                .header(CONTENT_LENGTH, file.objectMetadata["Content-Length"]!!)
                .header(
                    "Content-Disposition",
                    "inline; filename=\"$customFileName\""
                )
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .eTag(file.objectMetadata["eTag"]) // lastModified is also ava
                .body(InputStreamResource(file.inputStream))
        } else {
            throw InvalidQuestionId()
        }
    }

    fun deleteResponse(surveyId: UUID, responseId: UUID) {
        val response = responseRepository.findByIdOrNull(responseId) ?: throw ResponseNotFoundException()
        helper.responseFiles(surveyId, responseId).forEach { file ->
            helper.delete(surveyId, SurveyFolder.Responses(responseId.toString()), file.name)
        }
        responseRepository.delete(response)
    }

}
