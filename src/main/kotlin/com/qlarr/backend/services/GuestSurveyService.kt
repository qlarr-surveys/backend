package com.qlarr.backend.services

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.qlarr.backend.api.survey.CloneRequest
import com.qlarr.backend.api.survey.SimpleSurveyDto
import com.qlarr.backend.api.survey.Status
import com.qlarr.backend.api.survey.SurveyDTO
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.nowUtc
import com.qlarr.backend.exceptions.DesignNotAvailableException
import com.qlarr.backend.exceptions.DuplicateSurveyException
import com.qlarr.backend.exceptions.SurveyDefNotAvailableException
import com.qlarr.backend.helpers.FileSystemHelper
import com.qlarr.backend.mappers.SurveyMapper
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.repositories.SurveyRepository
import com.qlarr.backend.persistence.repositories.VersionRepository
import com.qlarr.expressionmanager.model.jacksonKtMapper
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


@Service
class GuestSurveyService(
    val surveyMapper: SurveyMapper,
    val restTemplate: RestTemplate,
    val fileSystemHelper: FileSystemHelper,
    val versionRepository: VersionRepository,
    val surveyRepository: SurveyRepository,
    @Value("\${enterprise.domain}") val enterpriseDomain: String
) {

    @Transactional
    fun cloneGuestSurvey(surveyId: UUID, cloneRequest: CloneRequest): SurveyDTO {
        val url = "$enterpriseDomain/guest/survey/$surveyId/clone_to_local"
        val response = restTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            throw ResponseStatusException(response.statusCode, "Cloning guest survey to local failed")
        }

        val byteArrayInputStream = ByteArrayInputStream(response.body)
        val bufferedInputStream = BufferedInputStream(byteArrayInputStream)
        val zipInputStream = ZipInputStream(bufferedInputStream)

        var surveyDTO: SurveyDTO? = null
        var designSaved = false

        val newId = UUID.randomUUID()

        zipInputStream.use {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                println(
                    "name: ${zipEntry.name}, fileName: ${extractFileName(zipEntry.name)}, parentFolderName: ${
                        extractParentFolderName(
                            zipEntry.name
                        )
                    }"
                )
                if (zipEntry.isDirectory) {
                    continue
                } else {
                    val fileName = extractFileName(zipEntry.name)
                    if (fileName == "survey.json") {
                        surveyDTO = saveSurveyData(it, cloneRequest)
                    } else if (extractParentFolderName(zipEntry.name).equals("resources")) {
                        unzipFileToFileSystem(newId, SurveyFolder.RESOURCES, it, fileName, fileName)
                    } else if (fileName == "design.json") {
                        unzipFileToFileSystem(newId, SurveyFolder.DESIGN, it, fileName, "1")
                        designSaved = true
                    }
                }
                zipEntry = it.nextEntry
            }
        }
        if (!designSaved) {
            throw DesignNotAvailableException()
        }
        if (surveyDTO == null) {
            throw SurveyDefNotAvailableException()
        }
        fileSystemHelper.changeSurveyDirectory(newId.toString(), surveyDTO!!.id.toString())
        return surveyDTO ?: throw SurveyDefNotAvailableException()

    }

    private fun extractParentFolderName(path: String): String? = path.split("/")
        .takeIf { it.size >= 2 }?.let { it[0] }


    fun extractFileName(path: String): String = path.split("/").let { it[it.size - 1] }

    fun unzipFileToFileSystem(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        zipInputStream: ZipInputStream,
        currentFileName: String,
        newFileName: String
    ) {
        val inputStream = ByteArrayInputStream(zipInputStream.readAllBytes())
        val mimeType = currentFileName.let { Files.probeContentType(File(it).toPath()) }
        fileSystemHelper.upload(surveyId, surveyFolder, inputStream, mimeType, newFileName)
    }

    fun saveSurveyData(zipInputStream: ZipInputStream, cloneRequest: CloneRequest): SurveyDTO {
        val surveyDataString = String(zipInputStream.readAllBytes())
        val simpleSurveyDto =
            jacksonKtMapper.registerModule(JavaTimeModule()).readValue(surveyDataString, SimpleSurveyDto::class.java)

        val savedSurvey = try {
            surveyRepository.save(
                SurveyEntity(
                    creationDate = simpleSurveyDto.creationDate,
                    lastModified = simpleSurveyDto.lastModified,
                    name = cloneRequest.name,
                    status = Status.DRAFT,
                    startDate = simpleSurveyDto.startDate,
                    endDate = simpleSurveyDto.endDate,
                    usage = simpleSurveyDto.usage,
                    quota = -1,
                    canLockSurvey = false,
                    image = simpleSurveyDto.image,
                    description = simpleSurveyDto.description
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