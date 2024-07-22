package com.frankie.backend.helpers

import com.frankie.backend.api.survey.FileInfo
import com.frankie.backend.common.SurveyFolder
import com.frankie.backend.exceptions.ResourceNotFoundException
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*


interface FileHelper {

    fun upload(
            surveyId: UUID,
            surveyFolder: SurveyFolder,
            file: MultipartFile,
            contentType: String,
            filename: String
    )

    fun generateETagUsingMetadata(file: File): String


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

    fun filerSurveyResources(
            surveyId: UUID,
            files: List<String>? = null,
            dateFrom: LocalDateTime? = null
    ):List<FileInfo>

    fun filerSurveyFiles(
            surveyId: UUID,
            surveyFolder: SurveyFolder,
            files: List<String>? = null,
            dateFrom: LocalDateTime? = null
    ): List<FileInfo>

    fun cloneResources(sourceSurveyId: UUID, destinationSurveyId: UUID)

    fun copyDesign(
            sourceSurveyId: UUID,
            destinationSurveyId: UUID,
            sourceFileName: String,
            newFileName: String)

    fun deleteSurveyFiles(surveyId: UUID)

    fun download(surveyId: UUID, surveyFolder: SurveyFolder, filename: String): FileDownload

    fun getText(surveyId: UUID, surveyFolder: SurveyFolder, filename: String):String


    fun delete(surveyId: UUID, surveyFolder: SurveyFolder, filename: String)


    companion object {
        const val METADATA_POSTFIX = ".metadata"
    }

}

class FileDownload(val objectMetadata: Map<String, String>, val inputStream: InputStream)