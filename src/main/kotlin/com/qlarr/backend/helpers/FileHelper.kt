package com.qlarr.backend.helpers

import com.qlarr.backend.api.response.ResponseEvent
import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.common.SurveyFolder
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.util.*


interface FileHelper {

    fun uploadUnzippedFile(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        inputStream: InputStream,
        contentType: String,
        filename: String
    )

    fun upload(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        file: MultipartFile,
        contentType: String,
        filename: String,
    ): String

    fun doesFileExists(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ): Boolean

    fun upload(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        text: String,
        filename: String
    )

    fun listSurveyResources(surveyId: UUID): List<FileInfo>

    fun surveyResourcesFiles(
        surveyId: UUID,
        files: List<String>? = null,
        dateFrom: LocalDateTime? = null
    ): List<FileInfo>

    fun responseFiles(
        surveyId: UUID,
        responseId: UUID
    ): List<FileInfo>

    fun cloneResources(sourceSurveyId: UUID, destinationSurveyId: UUID)

    fun copyDesign(
        sourceSurveyId: UUID,
        destinationSurveyId: UUID,
        sourceFileName: String,
        newFileName: String
    )

    fun deleteSurveyFiles(surveyId: UUID)

    fun download(surveyId: UUID, surveyFolder: SurveyFolder, filename: String): FileDownload

    fun getText(surveyId: UUID, surveyFolder: SurveyFolder, filename: String): String


    fun delete(surveyId: UUID, surveyFolder: SurveyFolder, filename: String)

    fun exportSurvey(surveyId: UUID, designFileName: String, surveyDataJson: String): ByteArray

    fun extractImportZip(inputStream: InputStream): ImportedSurveyZip

    fun uploadImportedSurvey(surveyId: UUID, designFile: File, resources: List<Pair<String, File>>)

    fun deleteUnusedResponseFiles(surveyId: UUID, responseId: UUID, values: Map<String, Any>, events: List<ResponseEvent>)

}

class FileDownload(val objectMetadata: Map<String, String>, val inputStream: InputStream)

data class ImportedSurveyZip(
    val surveyJson: String?,
    val designFile: File?,
    val resources: List<Pair<String, File>>
) {
    fun cleanup() {
        designFile?.delete()
        resources.forEach { it.second.delete() }
    }
}