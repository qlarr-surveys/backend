package com.qlarr.backend.helpers

import com.qlarr.backend.api.survey.FileInfo
import com.qlarr.backend.api.survey.SurveyDTO
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.exceptions.ResourceNotFoundException
import com.qlarr.backend.properties.FileSystemProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.function.ServerResponse.async
import java.io.*
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Component
class FileSystemHelper(private val fileSystemProperties: FileSystemProperties) : FileHelper {

    override fun uploadUnzippedFile(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        inputStream: InputStream,
        contentType: String,
        filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        val length = saveToFile(inputStream, path)
        saveMetadata(File(path), contentType, length)
    }

    override fun upload(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        file: MultipartFile,
        contentType: String,
        filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        if (file.isEmpty) {
            throw ResourceNotFoundException()
        }

        val byteStream = file.inputStream
        saveToFile(byteStream, path)
        saveMetadata(File(path), contentType, file.size)
    }

    private fun generateETagUsingMetadata(file: File): String {
        val lastModified = file.lastModified()
        val fileSize = file.length()
        return "$lastModified-$fileSize"
    }

    fun saveMetadata(file: File, contentType: String, length: Long) {
        val etag = generateETagUsingMetadata(file)
        val metadataFile = File(file.path + METADATA_POSTFIX)
        val path = Paths.get(file.parent)
        Files.createDirectories(path)
        if (!file.exists()) {
            file.createNewFile()
        }
        metadataFile.writeText("Content-Type: $contentType\netag: $etag\nContent-Length: $length")
    }

    private fun fetchMetadata(filePath: String): Map<String, String> {
        val metadataFile = File("$filePath$METADATA_POSTFIX")
        if (!metadataFile.exists()) {
            return emptyMap()
        }

        val metadata = mutableMapOf<String, String>()
        metadataFile.forEachLine { line ->
            val parts = line.split(":")
            if (parts.size == 2) {
                metadata[parts[0].trim()] = parts[1].trim()
            }
        }
        return metadata
    }

    override fun doesFileExists(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ): Boolean {
        val filePath = buildFilePath(surveyId, surveyFolder, filename)

        return File(filePath).exists()
    }

    override fun upload(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        text: String,
        filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        saveToFile(text.byteInputStream(), path)
    }

    override fun listSurveyResources(surveyId: UUID): List<FileInfo> {
        return surveyResourcesFiles(surveyId)
    }

    override fun surveyResourcesFiles(
        surveyId: UUID,
        files: List<String>?,
        dateFrom: LocalDateTime?
    ): List<FileInfo> {
        return surveyFiles(surveyId, SurveyFolder.RESOURCES, files, dateFrom)
    }

    private fun surveyFiles(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        files: List<String>?,
        dateFrom: LocalDateTime?
    ): List<FileInfo> {
        val surveyPath = buildFolderPath(surveyId, surveyFolder)
        val surveyDir = File(surveyPath)

        return surveyDir.listFiles()?.asIterable()
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
            } ?: emptyList()
    }

    override fun cloneResources(
        sourceSurveyId: UUID,
        destinationSurveyId: UUID
    ) {
        val sourceFolderPath = buildFolderPath(sourceSurveyId, SurveyFolder.RESOURCES)
        val sourceDir = File(sourceFolderPath)
        if (!sourceDir.exists())
            return

        val destinationFolderPath = buildFolderPath(destinationSurveyId, SurveyFolder.RESOURCES)
        val destinationDir = File(destinationFolderPath)

        FileUtils.copyDirectory(sourceDir, destinationDir)
    }

    override fun copyDesign(
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

    override fun deleteSurveyFiles(
        surveyId: UUID,
    ) {
        val surveyPath = buildFolderPath(surveyId)

        FileUtils.deleteDirectory(File(surveyPath))
    }

    override fun download(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ): FileDownload {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        val metadata = fetchMetadata(path)

        return FileDownload(metadata, File(path).inputStream())
    }

    override fun getText(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ): String {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        return File(path).inputStream().use { inputStream ->
            IOUtils.toString(inputStream, Charsets.UTF_8)
        }
    }


    override fun delete(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)

        FileUtils.delete(File(path + METADATA_POSTFIX))
        FileUtils.delete(File(path))
    }

    fun isFolderNotEmpty(surveyId: UUID, surveyFolder: SurveyFolder): Boolean {
        return File(buildFolderPath(surveyId, surveyFolder)).run {
            isDirectory && (listFiles()?.isNotEmpty() ?: false)
        }

    }

    fun zipFolder(surveyId: UUID, surveyFolder: SurveyFolder, zipOutputStream: ZipOutputStream) {
        return File(buildFolderPath(surveyId, surveyFolder)).run {
            listFiles()!!.filter { file ->
                !file.name.endsWith(METADATA_POSTFIX)
            }.forEach { file ->
                zipOutputStream.putNextEntry(ZipEntry("${surveyFolder.path}/${file.name}"))
                IOUtils.copy(FileInputStream(file), zipOutputStream)
                zipOutputStream.closeEntry()
            }
        }
    }

    override fun exportSurvey(surveyId: UUID, designFileName: String, surveyDataJson: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val bufferedOutputStream = BufferedOutputStream(byteArrayOutputStream)
        val zipOutputStream = ZipOutputStream(bufferedOutputStream)

        zipOutputStream.use { zipOut ->
            if (isFolderNotEmpty(surveyId, SurveyFolder.RESOURCES)) {
                zipFolder(surveyId, SurveyFolder.RESOURCES, zipOut)
            }

            val designFile = File(buildFilePath(surveyId, SurveyFolder.DESIGN, designFileName))
            if (designFile.exists()) {
                val designFilePath = "design.json"
                zipOut.putNextEntry(ZipEntry(designFilePath))
                IOUtils.copy(FileInputStream(designFile), zipOut)
                zipOut.closeEntry()
            }

            zipOut.putNextEntry(ZipEntry("survey.json"))
            zipOut.write(surveyDataJson.toByteArray())
            zipOut.closeEntry()
        }

        return byteArrayOutputStream.toByteArray()
    }

    override fun importSurvey(
        inputStream: InputStream,
        onSurveyData: (String) -> SurveyDTO,
        onDesign: () -> Unit
    ) {
        val zipInputStream = ZipInputStream(inputStream)
        val tempFiles = mutableListOf<File>()
        val uploadTasks = mutableListOf<() -> Unit>()


        val newId = UUID.randomUUID()
        var newSurveyId: UUID? = null

        try {
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
                    if (!zipEntry.isDirectory) {
                        val fileName = extractFileName(zipEntry.name)
                        if (fileName == "survey.json") {
                            val surveyDataString = zipInputStream.bufferedReader().readText()
                            newSurveyId = onSurveyData(surveyDataString).id
                        } else if (extractParentFolderName(zipEntry.name).equals("resources")) {
                            val tempFile = kotlin.io.path.createTempFile("upload_", "_$fileName").toFile()
                            tempFiles.add(tempFile)
                            tempFile.outputStream().use { output ->
                                zipInputStream.copyTo(output)
                            }
                            uploadTasks.add {
                                tempFile.inputStream().use { fileInput ->
                                    unzipFileToFileSystem(newId, SurveyFolder.RESOURCES, fileInput, fileName, fileName)
                                }
                            }
                        } else if (fileName == "design.json") {
                            val tempFile = kotlin.io.path.createTempFile("upload_", "_design.json").toFile()
                            tempFiles.add(tempFile)
                            tempFile.outputStream().use { output ->
                                zipInputStream.copyTo(output)
                            }
                            uploadTasks.add {
                                tempFile.inputStream().use { fileInput ->
                                    unzipFileToFileSystem(
                                        newId,
                                        SurveyFolder.DESIGN,
                                        fileInput,
                                        getMimeType(fileName),
                                        "1"
                                    )
                                }
                            }
                            onDesign()
                        }
                    }
                    zipEntry = it.nextEntry
                }
            }
            runBlocking {
                uploadTasks.map { task ->
                    async(Dispatchers.IO) {
                        task()
                    }
                }.awaitAll()
            }
            changeSurveyDirectory(newId.toString(), newSurveyId.toString())
        } finally {
            tempFiles.forEach { it.delete() }
        }
    }

    private fun getMimeType(fileName: String): String {
        return URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
    }

    private fun unzipFileToFileSystem(
        surveyId: UUID,
        surveyFolder: SurveyFolder,
        zipInputStream: InputStream,
        currentFileName: String,
        newFileName: String
    ) {
        val mimeType = getMimeType(currentFileName)
        uploadUnzippedFile(surveyId, surveyFolder, zipInputStream, mimeType, newFileName)
    }

    private fun extractFileName(path: String): String = path.split("/").let { it[it.size - 1] }
    private fun extractParentFolderName(path: String): String? = path.split("/")
        .takeIf { it.size >= 2 }?.let { it[0] }

    private fun saveToFile(inputStream: InputStream, path: String): Long {
        val typedPath = Paths.get(path)
        Files.createDirectories(typedPath.parent)

        var totalBytes = 0L
        val buffer = ByteArray(8192) // 8KB buffer

        Files.newOutputStream(typedPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { outputStream ->
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
        }

        return totalBytes
    }

    private fun buildFilePath(surveyId: UUID, surveyFolder: SurveyFolder, filename: String) =
        "${fileSystemProperties.rootFolder}/$surveyId/${surveyFolder.path}/$filename"

    private fun buildFolderPath(surveyId: UUID, surveyFolder: SurveyFolder) =
        "${fileSystemProperties.rootFolder}/$surveyId/${surveyFolder.path}"


    private fun buildFolderPath(surveyId: UUID) = "${fileSystemProperties.rootFolder}/$surveyId"

    fun changeSurveyDirectory(from: String, to: String): Boolean {
        val oldDir = File("${fileSystemProperties.rootFolder}/$from")
        val newDir = File("${fileSystemProperties.rootFolder}/$to")

        // Ensure the old directory exists and is a directory
        if (oldDir.exists() && oldDir.isDirectory) {
            // Rename the directory (move to new path)
            return oldDir.renameTo(newDir)
        }
        return false
    }

    companion object {
        const val METADATA_POSTFIX = ".metadata"
    }

}