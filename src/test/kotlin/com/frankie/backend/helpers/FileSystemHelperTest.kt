package com.frankie.backend.helpers

import com.frankie.backend.api.survey.FileInfo
import com.frankie.backend.common.SurveyFolder
import com.frankie.backend.properties.FileSystemProperties
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.time.LocalDateTime
import java.util.*

class FileSystemHelperTest {

    private val rootDir = "src/test/resources/root"
    private val fileSystemHelper: FileHelper = FileSystemHelper(FileSystemProperties(rootDir))

    @AfterEach
    fun setUp() {
        FileUtils.cleanDirectory(File(rootDir))
    }

    @Test
    fun upload_should_uploadExistingFileToDirectory() {
        val file = File("src/test/resources/files/photo.png")
        val mock = MockMultipartFile("photo.png", file.inputStream())
        val surveyId = UUID.randomUUID()
        val fileName = "test.png"

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, mock, "", fileName)

        assertThat(fileSystemHelper.doesFileExists(surveyId, SurveyFolder.RESOURCES, fileName)).isTrue
    }

    @Test
    fun upload_should_createFileAndUploadToDirectory() {
        val surveyId = UUID.randomUUID()
        val fileName = "test.txt"
        val text = "TEST123"

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, text, fileName)

        assertThat(fileSystemHelper.doesFileExists(surveyId, SurveyFolder.RESOURCES, fileName)).isTrue
        assertThat(fileSystemHelper.getText(surveyId, SurveyFolder.RESOURCES, fileName)).isEqualTo(text)
    }

    @Test
    fun listSurveyResources_should_listAllFilesFromResourcesFolder() {
        val surveyId = UUID.randomUUID()
        val fileName = "test.txt"

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", fileName)

        val resources = fileSystemHelper.listSurveyResources(surveyId)

        assertThat(resources).hasSize(1)
                .extracting(FileInfo::name).containsOnly(tuple(fileName))
    }

    @Test
    fun filerSurveyResources_should_notFilterAnyFile_when_parametersAreNull() {
        val surveyId = UUID.randomUUID()

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", "test1.txt")
        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST321", "test2.txt")

        val resources = fileSystemHelper.filerSurveyResources(surveyId)

        assertThat(resources).hasSize(2)
    }

    @Test
    fun filerSurveyResources_should_filterFilesByName() {
        val surveyId = UUID.randomUUID()
        val fileName = "test.txt"

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", fileName)

        val resources = fileSystemHelper.filerSurveyResources(surveyId, files = listOf(fileName))

        assertThat(resources).hasSize(1)
                .extracting(FileInfo::name).containsOnly(tuple(fileName))
    }

    @Test
    fun filerSurveyResources_should_filterByLastModifiedTimestamp() {
        val surveyId = UUID.randomUUID()

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", "test.txt")

        val result1 = fileSystemHelper.filerSurveyResources(surveyId, dateFrom = LocalDateTime.now().minusHours(1))
        val result2 = fileSystemHelper.filerSurveyResources(surveyId, dateFrom = LocalDateTime.now().plusHours(1))

        assertThat(result1).hasSize(1)
        assertThat(result2).isEmpty()
    }

    @Test
    fun cloneResources_should_cloneFilesFromOneResourceFolderToAnother() {
        val sourceSurveyId = UUID.randomUUID()
        val fileName1 = "test1.txt"
        val fileName2 = "test2.txt"

        // prepare files for cloning
        fileSystemHelper.upload(sourceSurveyId, SurveyFolder.RESOURCES, "TEST123", fileName1)
        fileSystemHelper.upload(sourceSurveyId, SurveyFolder.RESOURCES, "TEST321", fileName2)

        val destinationSurveyId = UUID.randomUUID()

        fileSystemHelper.cloneResources(sourceSurveyId, destinationSurveyId)

        val resources = fileSystemHelper.listSurveyResources(destinationSurveyId)

        assertThat(resources).hasSize(2)
                .extracting(FileInfo::name).containsExactlyInAnyOrder(tuple(fileName1), tuple(fileName2))
    }

    @Test
    fun copyDesign_should_copyFileFromOneSurveyDesignFolderIntoAnother() {
        val sourceSurveyId = UUID.randomUUID()
        val sourceFileName = "test1.txt"
        val newFileName = "new_file.txt"

        // prepare files for copying
        fileSystemHelper.upload(sourceSurveyId, SurveyFolder.DESIGN, "TEST123", sourceFileName)

        val destinationSurveyId = UUID.randomUUID()

        fileSystemHelper.copyDesign(sourceSurveyId, destinationSurveyId, sourceFileName, newFileName)

        val resources = fileSystemHelper.filerSurveyFiles(destinationSurveyId, SurveyFolder.DESIGN)

        assertThat(resources).hasSize(1)
                .extracting(FileInfo::name).containsExactlyInAnyOrder(tuple(newFileName))
    }

    @Test
    fun deleteSurveyFiles_should_deleteAllSurveyFiles() {
        val surveyId = UUID.randomUUID()
        val filename = "test1.txt"

        // create survey folder with some files to delete
        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", filename)

        fileSystemHelper.deleteSurveyFiles(surveyId)

        val doesExist = fileSystemHelper.doesFileExists(surveyId, SurveyFolder.RESOURCES, filename)

        assertThat(doesExist).isFalse
        assertThat(File("$rootDir/$surveyId").exists()).isFalse
    }

    @Test
    fun download_should_fetchFileInputStreamFromFileSystem() {
        val surveyId = UUID.randomUUID()
        val filename = "test1.txt"
        val text = "TEST123"

        // create survey folder with some files to delete
        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, text, filename)

        val inputStream = fileSystemHelper.download(surveyId, SurveyFolder.RESOURCES, filename)

        val fileContent = inputStream.inputStream.use { stream -> String(stream.readAllBytes()) }

        assertThat(fileContent).isEqualTo(text)
    }


}