package com.qlarr.backend.services

import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.common.RandomResourceIdGenerator
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.exceptions.ResourceNotFoundException
import com.qlarr.backend.exceptions.SurveyIsClosedException
import com.qlarr.backend.exceptions.SurveyNotFoundException
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.persistence.repositories.SurveyRepository
import org.springframework.core.io.InputStreamResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders.*
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class SurveyResourceService(
    private val helper: FileHelper,
    private val surveyRepository: SurveyRepository,
) {
    fun uploadResource(surveyId: UUID, file: MultipartFile): ResponseEntity<FileInfo> {
        surveyRepository.findByIdOrNull(surveyId)?.let { surveyEntity ->
            if (surveyEntity.status == Status.CLOSED) {
                throw SurveyIsClosedException()
            }
        } ?: throw SurveyNotFoundException()
        val filename = RandomResourceIdGenerator.generateRandomIdWithExtension(file)
        val mimeType = file.contentType
            ?: file.originalFilename?.let { Files.probeContentType(File(it).toPath()) }
            ?: "application/octet-stream"
        val savedFilename = helper.upload(surveyId, SurveyFolder.Resources, file, mimeType, filename)
        return ResponseEntity.ok().body(FileInfo(savedFilename, file.size, nowUtc()))
    }


    fun downloadResource(serverName: String, surveyId: UUID, fileName: String): ResponseEntity<InputStreamResource> {
        surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val response = helper.download(surveyId, SurveyFolder.Resources, fileName)
        return ResponseEntity.ok()
            .header(CONTENT_TYPE, response.objectMetadata["Content-Type"]!!)
            .header(CONTENT_LENGTH, response.objectMetadata["Content-Length"]!!)
            .header(ACCEPT_RANGES, "bytes")
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
            helper.delete(surveyId, SurveyFolder.Resources, fileName)
            return ResponseEntity.ok().body("Survey resource deleted Successfully")
        } catch (e: ResourceNotFoundException) {
            throw ResourceNotFoundException()
        }
    }
}
