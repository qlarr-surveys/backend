package com.qlarr.backend.services

import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.helpers.FileSystemHelper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
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
            val zipEntry: ZipEntry? = it.getNextEntry()
            while (zipEntry != null) {
                val inputStream = ByteArrayInputStream(zipInputStream.readAllBytes())
                fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, inputStream, zipEntry.name)
            }
        }

        return true
    }


}