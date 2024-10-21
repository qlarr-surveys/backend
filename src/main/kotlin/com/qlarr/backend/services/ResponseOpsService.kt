package com.qlarr.backend.services

import com.qlarr.backend.api.response.*
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.UserUtils
import com.qlarr.backend.exceptions.*
import com.qlarr.backend.expressionmanager.SurveyProcessor
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.expressionmanager.model.*
import com.qlarr.expressionmanager.usecase.SurveyDesignWithErrorException
import org.springframework.core.io.InputStreamResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.CacheControl
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
        helper.upload(surveyId, SurveyFolder.RESPONSES, file, file.contentType ?: "", fileName)
        val responseUploadFile = ResponseUploadFile(
                file.originalFilename!!, fileName, file.size, file.contentType
                ?: ""
        )
        val newValues = response.values.toMutableMap()
                .apply { put("$questionId.value", jacksonKtMapper.convertValue(responseUploadFile, Map::class.java)) }
        responseRepository.save(response.copy(values = newValues))
        return responseUploadFile
    }

    fun uploadOfflineResponseFile(
            surveyId: UUID,
            filename: String,
            file: MultipartFile
    ): ResponseUploadFile {
        val survey = surveyService.getSurveyById(surveyId)
        if (survey.status != Status.ACTIVE) {
            throw SurveyIsNotActiveException()
        }
        val mimeType = file.originalFilename!!.let { Files.probeContentType(File(it).toPath()) }
        helper.upload(surveyId, SurveyFolder.RESPONSES, file, mimeType, filename)
        return ResponseUploadFile(filename, filename, file.size, file.contentType ?: "")
    }

    fun isOfflineFileAlreadyUploaded(
            surveyId: UUID,
            filename: String
    ): Boolean {
        return helper.doesFileExists(surveyId, SurveyFolder.RESPONSES, filename)
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

        val navigationUseCaseInput = NavigationUseCaseInput(
                values = uploadResponseRequestData.values,
                navigationInfo = NavigationInfo(
                        navigationDirection = NavigationDirection.Resume,
                        navigationIndex = uploadResponseRequestData.navigationIndex
                ),
                lang = uploadResponseRequestData.lang,
        )
        val navigationResult =
                SurveyProcessor.navigate(
                        survey.validationJsonOutput,
                        navigationUseCaseInput,
                        false,
                        SurveyMode.OFFLINE
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
            filename: UUID
    ): ResponseEntity<InputStreamResource> {
        val file = helper.download(surveyId, SurveyFolder.RESPONSES, filename.toString())
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .header(CONTENT_TYPE, file.objectMetadata["Content-Type"]!!)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .eTag(file.objectMetadata["eTag"]) // lastModified is also ava
                .body(InputStreamResource(file.inputStream))
    }


    @Suppress("UNCHECKED_CAST")
    fun deleteResponse(surveyId: UUID, responseId: UUID) {
        val response = responseRepository.findByIdOrNull(responseId) ?: throw ResponseNotFoundException()
        val processedSurvey = designService.getProcessedSurveyByVersion(surveyId, response.version)
        processedSurvey.validationJsonOutput.schema
                .filter { it.dataType == DataType.FILE }
                .forEach { responseField ->
                    response.values[responseField.toValueKey()]?.let {
                        (it as Map<String, String>)["stored_filename"]?.let { storedFileName ->
                            helper.delete(surveyId, SurveyFolder.RESPONSES, storedFileName)
                        }
                    }

                }
        responseRepository.delete(response)
    }

}
