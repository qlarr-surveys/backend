package com.frankie.backend.helpers

import com.frankie.backend.api.survey.FileInfo
import com.frankie.backend.common.SurveyFolder
import com.frankie.backend.exceptions.ResourceNotFoundException
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

private const val testTenantID = "63927513-d9e5-48fe-a07b-e7b5ab284947"

@Component
class FileSystemHelper(private val workingDir: String) {

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
        val surveyPath = buildFolderPath(surveyId, SurveyFolder.RESOURCES)
        val surveyFolder = File(surveyPath)

        return surveyFolder.listFiles()?.asIterable()?.map { file ->
            FileInfo(
                file.name,
                file.length(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault())
            )
        } ?: throw ResourceNotFoundException()
    }

    fun filerSurveyResources(
        surveyId: UUID,
        files: List<String>? = null,
        dateFrom: LocalDateTime? = null
    ): List<FileInfo> {
        val surveyPath = buildFolderPath(surveyId, SurveyFolder.RESOURCES)
        val surveyFolder = File(surveyPath)

        return surveyFolder.listFiles()?.asIterable()
            ?.filter { file ->
                dateFrom?.isBefore(
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(file.lastModified()),
                        ZoneId.systemDefault()
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
        deleteFolder(surveyPath)
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

    private fun deleteFolder(
        path: String
    ) {
        FileUtils.deleteDirectory(File(path))
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
            "$workingDir/%s/%s/%s/%s",
            testTenantID,
            surveyId.toString(),
            surveyFolder.path,
            filename
        )

    private fun buildFolderPath(surveyId: UUID, surveyFolder: SurveyFolder) =
        String.format(
            "$workingDir/%s/%s/%s",
            testTenantID,
            surveyId.toString(),
            surveyFolder.path,
        )

    private fun buildFolderPath(surveyId: UUID) =
        String.format(
            "$workingDir/%s/%s/%s",
            testTenantID,
            surveyId.toString()
        )

}