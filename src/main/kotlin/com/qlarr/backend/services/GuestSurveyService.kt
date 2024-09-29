package com.qlarr.backend.services

import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.helpers.FileSystemHelper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
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
    @Value("\${enterprise.domain}") val enterpriseDomain: String
) {

    fun cloneGuestSurvey(surveyId: UUID): Boolean {
        val url = "$enterpriseDomain/guest/survey/$surveyId/clone_to_local"
        val response = restTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            return false
        }

        val byteArrayInputStream = ByteArrayInputStream(response.body)
        val bufferedInputStream = BufferedInputStream(byteArrayInputStream)
        val zipInputStream = ZipInputStream(bufferedInputStream)
        zipInputStream.use {
            do {
                val zipEntry: ZipEntry? = it.getNextEntry()
                val path = zipEntry?.name?.split(File.pathSeparator)
                val fileName = path?.get(path.size - 2)
                if (zipEntry?.isDirectory == true && fileName == "resources") {
                    unzipFolder(surveyId, SurveyFolder.RESOURCES, it)
                }

                if (zipEntry?.isDirectory == true && fileName == "design") {
                    unzipFolder(surveyId, SurveyFolder.DESIGN, it)
                }

            } while (zipEntry != null)
        }

        return true
    }

    fun unzipFolder(surveyId: UUID, surveyFolder: SurveyFolder, zipInputStream: ZipInputStream) {
        val zipEntry: ZipEntry? = zipInputStream.getNextEntry()
        while (zipEntry != null && !zipEntry.isDirectory) {
            val inputStream = ByteArrayInputStream(zipInputStream.readAllBytes())
            fileSystemHelper.upload(surveyId, surveyFolder, inputStream, zipEntry.name)

            zipInputStream.nextEntry
        }
    }
}