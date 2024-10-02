package com.qlarr.backend.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qlarr.backend.api.survey.SimpleSurveyDto
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.helpers.FileSystemHelper
import com.qlarr.backend.persistence.entities.VersionEntity
import com.qlarr.backend.persistence.repositories.VersionRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


@Service
class GuestSurveyService(
    val restTemplate: RestTemplate,
    val fileSystemHelper: FileSystemHelper,
    val versionRepository: VersionRepository,
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
            do {
                val zipEntry: ZipEntry? = it.getNextEntry()
                if (zipEntry?.isDirectory == true) {
                    continue
                } else {
                    if (extractFileName(zipEntry?.name).equals("survey.json")) {
                        saveSurveyData(surveyId, it)
                    } else if (extractParentFolderName(zipEntry?.name).equals("resources")) {
                        unzipFileToFileSystem(surveyId, SurveyFolder.RESOURCES, it, extractFileName(zipEntry?.name))
                    } else if (extractFileName(zipEntry?.name).equals("design.json")) {
                        unzipFileToFileSystem(surveyId, SurveyFolder.DESIGN, it, "1")
                    }
                }
            } while (zipEntry != null)
        }
    }

    fun extractParentFolderName(path: String?): String? {
        val pathArray = path?.split(File.pathSeparator)
        return pathArray?.get(pathArray.size - 2)
    }

    fun extractFileName(path: String?): String? {
        val pathArray = path?.split(File.pathSeparator)
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
        val simpleSurveyDto = jacksonObjectMapper().readValue(surveyDataString, SimpleSurveyDto::class.java)

        versionRepository.save(
            simpleSurveyDto.latestVersion.let {
                VersionEntity(version = it.version,
                    surveyId = it.surveyId,
                    subVersion = it.subVersion,
                    valid = it.valid,
                    published = it.published,
                    schema = listOf(),
                    lastModified = it.lastModified)
            }
        )
    }
}