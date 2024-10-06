package com.qlarr.backend.services

import com.qlarr.backend.api.survey.SimpleSurveyDto
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.helpers.FileSystemHelper
import com.qlarr.backend.persistence.entities.SurveyEntity
import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.repositories.SurveyRepository
import com.qlarr.backend.persistence.repositories.VersionRepository
import com.qlarr.expressionmanager.model.jacksonKtMapper
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


@Service
class GuestSurveyService(
    val restTemplate: RestTemplate,
    val fileSystemHelper: FileSystemHelper,
    val versionRepository: VersionRepository,
    val surveyRepository: SurveyRepository,
    @Value("\${enterprise.domain}") val enterpriseDomain: String
) {

    @Transactional
    fun cloneGuestSurvey(surveyId: UUID) {
        val url = "$enterpriseDomain/guest/survey/$surveyId/clone_to_local"
        val response = restTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            throw ResponseStatusException(response.statusCode, "Cloning guest survey to local failed")
        }

        val byteArrayInputStream = ByteArrayInputStream(response.body)
        val bufferedInputStream = BufferedInputStream(byteArrayInputStream)
        val zipInputStream = ZipInputStream(bufferedInputStream)

        zipInputStream.use {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                if (zipEntry.isDirectory) {
                    continue
                } else {
                    if (extractFileName(zipEntry.name).equals("survey.json")) {
                        saveSurveyData(surveyId, it)
                    } else if (extractParentFolderName(zipEntry.name).equals("resources")) {
                        unzipFileToFileSystem(surveyId, SurveyFolder.RESOURCES, it, extractFileName(zipEntry.name))
                    } else if (extractFileName(zipEntry.name).equals("design.json")) {
                        unzipFileToFileSystem(surveyId, SurveyFolder.DESIGN, it, "1")
                    }
                }
                zipEntry = it.nextEntry
            }
        }
    }

    fun extractParentFolderName(path: String?): String? {
        val pathArray = path?.split("/")
        if (pathArray == null || pathArray.size < 2) {
            return null
        }
        return pathArray[pathArray.size - 2]
    }

    fun extractFileName(path: String?): String? {
        val pathArray = path?.split("/")
        return pathArray?.get(pathArray.size - 1)
    }

    fun unzipFileToFileSystem(surveyId: UUID, surveyFolder: SurveyFolder, zipInputStream: ZipInputStream, newFileName: String?) {
        val inputStream = ByteArrayInputStream(zipInputStream.readAllBytes())
        if (newFileName == null) {
            throw RuntimeException("New file name cannot be null!")
        }

        fileSystemHelper.upload(surveyId, surveyFolder, inputStream, newFileName)
    }

    fun saveSurveyData(surveyId: UUID, zipInputStream: ZipInputStream) {
        val surveyDataString = String(zipInputStream.readAllBytes())
        val simpleSurveyDto = jacksonKtMapper.readValue(surveyDataString, SimpleSurveyDto::class.java)

        val savedSurvey = surveyRepository.save(
            simpleSurveyDto.let {
                SurveyEntity(
                    id = it.id,
                    creationDate = it.creationDate,
                    lastModified = it.lastModified,
                    name = it.name,
                    status = it.status,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    usage = it.usage,
                    quota = it.surveyQuota,
                    canLockSurvey = false,
                    image = it.image,
                    description = it.description
                )
            }
        )

        versionRepository.save(
            simpleSurveyDto.latestVersion.let {
                VersionEntity(version = it.version,
                    surveyId = savedSurvey.id!!,
                    subVersion = it.subVersion,
                    valid = it.valid,
                    published = it.published,
                    schema = listOf(),
                    lastModified = it.lastModified)
            }
        )
    }
}