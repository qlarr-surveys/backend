package com.frankie.backend.services

import com.frankie.backend.api.survey.FileInfo
import com.frankie.backend.api.survey.Status
import com.frankie.backend.common.SurveyFolder
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.ResourceNotFoundException
import com.frankie.backend.exceptions.SurveyIsClosedException
import com.frankie.backend.exceptions.SurveyNotFoundException
import com.frankie.backend.helpers.FileSystemHelper
import com.frankie.backend.persistence.repositories.SurveyRepository
import org.apache.http.protocol.HTTP.CONTENT_TYPE
import org.springframework.core.io.InputStreamResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class SurveyResourceService(
        private val helper: FileSystemHelper,
        private val surveyRepository: SurveyRepository,
) {
    fun uploadResource(surveyId: UUID, file: MultipartFile): ResponseEntity<FileInfo> {
        surveyRepository.findByIdOrNull(surveyId)?.let { surveyEntity ->
            if (surveyEntity.status == Status.CLOSED) {
                throw SurveyIsClosedException()
            }
        } ?: throw SurveyNotFoundException()
        helper.upload(surveyId, SurveyFolder.RESOURCES, file, file.contentType ?: "", file.originalFilename!!)
        return ResponseEntity.ok().body(FileInfo(file.originalFilename!!, file.size, nowUtc()))
    }


    fun downloadResource(serverName: String, surveyId: UUID, fileName: String): ResponseEntity<InputStreamResource> {
        surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val response = helper.download(surveyId, SurveyFolder.RESOURCES, fileName)
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .header(CONTENT_TYPE, response.objectMetadata["content-type"]!!)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .eTag(response.objectMetadata["eTag"]) // lastModified is also ava
                .body(InputStreamResource(response.inputStream))
    }

    fun removeResource(surveyId: UUID, fileName: String): ResponseEntity<Any> {
        surveyRepository.findByIdOrNull(surveyId)?.let { surveyEntity ->
            if (surveyEntity.status == Status.CLOSED) {
                throw SurveyIsClosedException()
            }
        } ?: throw SurveyNotFoundException()
        try {
            helper.delete(surveyId, SurveyFolder.RESOURCES, fileName)
            return ResponseEntity.ok().body("Survey resource deleted Successfully")
        } catch (e: ResourceNotFoundException) {
            throw ResourceNotFoundException()
        }
    }
}
