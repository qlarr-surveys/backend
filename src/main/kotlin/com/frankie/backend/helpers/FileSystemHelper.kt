package com.frankie.backend.helpers

import com.frankie.backend.api.survey.FileInfo
import com.frankie.backend.common.SurveyFolder
import com.frankie.backend.exceptions.ResourceNotFoundException
import com.frankie.backend.properties.FileSystemProperties
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Component
class FileSystemHelper(private val fileSystemProperties: FileSystemProperties) {

    fun upload(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        file: MultipartFile,
        filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        if (file.isEmpty) {
            throw ResourceNotFoundException()
        }

        val byteStream = file.inputStream
        saveToFile(byteStream, path)
    }

    fun doesFileExists(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ): Boolean {
        val filePath = buildFilePath(surveyId, surveyFolder, filename)

        return File(filePath).exists()
    }

    fun upload(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        text: String,
        filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        saveToFile(text.byteInputStream(), path)
    }

    fun listSurveyResources(surveyId: UUID): List<FileInfo> {
        return filerSurveyResources(surveyId)
    }

    fun filerSurveyResources(
        surveyId: UUID,
        files: List<String>? = null,
        dateFrom: LocalDateTime? = null
    ): List<FileInfo> {
        return filerSurveyFiles(surveyId, SurveyFolder.RESOURCES, files, dateFrom)
    }

    fun filerSurveyFiles(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        files: List<String>? = null,
        dateFrom: LocalDateTime? = null
    ): List<FileInfo> {
        val surveyPath = buildFolderPath(surveyId, surveyFolder)
        val surveyDir = File(surveyPath)

        return surveyDir.listFiles()?.asIterable()
            ?.filter { file ->
                dateFrom?.isBefore(
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(file.lastModified()),
                        ZoneId.systemDefault() // Zone changed to system default from UTC, since we are comparing to date from file system and not from S3
                    )
                ) ?: true && files?.contains(file.name) ?: true
            }?.map { file ->
                FileInfo(
                    file.name,
                    file.length(),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault())
                )
            } ?: throw ResourceNotFoundException()
    }

    fun cloneResources(
        sourceSurveyId: UUID,
        destinationSurveyId: UUID
    ) {
        val sourceFolderPath = buildFolderPath(sourceSurveyId, SurveyFolder.RESOURCES)
        val destinationFolderPath = buildFolderPath(destinationSurveyId, SurveyFolder.RESOURCES)

        val sourceDir = File(sourceFolderPath)
        val destinationDir = File(destinationFolderPath)

        FileUtils.copyDirectory(sourceDir, destinationDir)
    }

    fun copyDesign(
        sourceSurveyId: UUID,
        destinationSurveyId: UUID,
        sourceFileName: String,
        newFileName: String
    ) {
        val sourceFolderPath = buildFolderPath(sourceSurveyId, SurveyFolder.DESIGN)
        val destinationFolderPath = buildFolderPath(destinationSurveyId, SurveyFolder.DESIGN)

        val sourceFile = File("$sourceFolderPath/$sourceFileName")
        val destinationFile = File("$destinationFolderPath/$newFileName")

        FileUtils.copyFile(sourceFile, destinationFile)
    }

    fun deleteSurveyFiles(
        surveyId: UUID,
    ) {
        val surveyPath = buildFolderPath(surveyId)
        
        FileUtils.deleteDirectory(File(surveyPath))
    }

    fun download(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ): InputStream {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        return File(path).inputStream()
    }

    fun getText(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ): String {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        return File(path).inputStream().use { inputStream ->
            IOUtils.toString(inputStream, Charsets.UTF_8)
        }
    }


    fun delete(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        FileUtils.delete(File(path))
    }

    private fun saveToFile(byteStream: InputStream, path: String) {
        val p = Paths.get(path)

        Files.createDirectories(p.parent)

        byteStream.use { inputStream ->
            Files.copy(inputStream, Paths.get(path))
        }
    }

    private fun buildFilePath(surveyId: UUID, surveyFolder: SurveyFolder, filename: String) =
        String.format(
            "${fileSystemProperties.rootFolder}/%s/%s/%s",
            surveyId.toString(),
            surveyFolder.path,
            filename
        )

    private fun buildFolderPath(surveyId: UUID, surveyFolder: SurveyFolder) =
        String.format(
            "${fileSystemProperties.rootFolder}/%s/%s",
            surveyId.toString(),
            surveyFolder.path,
        )

    private fun buildFolderPath(surveyId: UUID) =
        String.format(
            "${fileSystemProperties.rootFolder}/%s",
            surveyId.toString()
        )

}