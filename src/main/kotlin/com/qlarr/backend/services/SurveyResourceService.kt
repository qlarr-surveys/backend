package com.qlarr.backend.services

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.qlarr.backend.api.survey.AutoCompleteFileInfo
import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.common.RandomResourceIdGenerator
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.configurations.objectMapper
import com.qlarr.backend.exceptions.ResourceNotFoundException
import com.qlarr.backend.exceptions.SurveyIsClosedException
import com.qlarr.backend.exceptions.SurveyNotFoundException
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.persistence.entities.AutoCompleteEntity
import com.qlarr.backend.persistence.repositories.AutoCompleteRepository
import com.qlarr.backend.persistence.repositories.SurveyRepository
import org.springframework.core.io.InputStreamResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders.CONTENT_LENGTH
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class SurveyResourceService(
    private val helper: FileHelper,
    private val surveyRepository: SurveyRepository,
    private val autoCompleteRepository: AutoCompleteRepository,
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

    private fun validateAutoCompleteFile(file: MultipartFile): ArrayNode {
        // Check file is not empty
        if (file.isEmpty) {
            throw IllegalArgumentException("File cannot be empty")
        }

        // Check file extension
        val filename = file.originalFilename ?: throw IllegalArgumentException("Filename is required")
        if (!filename.lowercase().endsWith(".json")) {
            throw IllegalArgumentException("File must be a JSON file")
        }

        // Parse and validate JSON structure
        try {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(file.inputStream)

            // Must be an array
            if (!jsonNode.isArray) {
                throw IllegalArgumentException("JSON file must contain an array at the root level")
            }

            val arrayNode = jsonNode as ArrayNode

            // Check if empty array
            if (arrayNode.isEmpty) {
                throw IllegalArgumentException("JSON array cannot be empty")
            }

            // Validate array contents
            val firstElement = arrayNode[0]

            when {
                firstElement.isTextual -> {
                    // Array of strings - validate all elements are strings
                    arrayNode.forEachIndexed { index, element ->
                        if (!element.isTextual) {
                            throw IllegalArgumentException(
                                "All array elements must be strings. Element at index $index is not a string"
                            )
                        }
                    }
                }

                firstElement.isObject -> {
                    // Array of objects - validate all have 'key' attribute with string value
                    arrayNode.forEachIndexed { index, element ->
                        if (!element.isObject) {
                            throw IllegalArgumentException(
                                "All array elements must be objects. Element at index $index is not an object"
                            )
                        }

                        val keyField = element.get("key")
                            ?: throw IllegalArgumentException(
                                "Object at index $index is missing required 'key' attribute"
                            )

                        if (!keyField.isTextual) {
                            throw IllegalArgumentException(
                                "Object at index $index has 'key' attribute that is not a string"
                            )
                        }
                    }
                }

                else -> {
                    throw IllegalArgumentException(
                        "JSON array must contain either strings or objects with a 'key' attribute"
                    )
                }
            }

            return arrayNode

        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException("Invalid JSON file: ${e.message}", e)
        } catch (e: IOException) {
            throw IllegalArgumentException("Error reading file: ${e.message}", e)
        }
    }

    fun uploadAutoCompleteResource(surveyId: UUID, componentId: String, file: MultipartFile): ResponseEntity<AutoCompleteFileInfo> {
        val arrayNode = validateAutoCompleteFile(file)
        surveyRepository.findByIdOrNull(surveyId)?.let { surveyEntity ->
            if (surveyEntity.status == Status.CLOSED) {
                throw SurveyIsClosedException()
            }
        } ?: throw SurveyNotFoundException()
        val entity = autoCompleteRepository.findBySurveyIdAndComponentId(surveyId, componentId)
            ?.copy(values = arrayNode)
            ?: AutoCompleteEntity(
                id = null,
                surveyId = surveyId,
                componentId = componentId,
                values = arrayNode
            )

        autoCompleteRepository.save(entity)

        val mimeType = file.contentType
            ?: file.originalFilename?.let { Files.probeContentType(File(it).toPath()) }
            ?: "application/octet-stream"
        val savedFilename = helper.upload(surveyId, SurveyFolder.Resources, file, mimeType, entity.id.toString())
        return ResponseEntity.ok().body(AutoCompleteFileInfo(
            name = savedFilename,
            rowCount = arrayNode.size(),
            size = file.size,
            lastModified = nowUtc()
        ))
    }


    fun downloadResource(serverName: String, surveyId: UUID, fileName: String): ResponseEntity<InputStreamResource> {
        surveyRepository.findByIdOrNull(surveyId) ?: throw SurveyNotFoundException()
        val response = helper.download(surveyId, SurveyFolder.Resources, fileName)
        return ResponseEntity.ok()
            .header(CONTENT_TYPE, response.objectMetadata["Content-Type"]!!)
            .header(CONTENT_LENGTH, response.objectMetadata["Content-Length"]!!)
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

    fun search(
        uuid: String,
        searchTerm: String,
        limit: Int = 10
    ): List<Any> {
        return autoCompleteRepository.searchAutoComplete(
            UUID.fromString(uuid),
            searchTerm,
            limit
        )
    }
}
