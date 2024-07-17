package com.frankie.backend.helpers

import com.frankie.backend.api.survey.FileInfo
import com.frankie.backend.common.SurveyFolder
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
    private val fileSystemHelper: FileSystemHelper = FileSystemHelper(rootDir)

    @AfterEach
    fun setUp() {
        FileUtils.cleanDirectory(File(rootDir))
    }

    @Test
    fun upload_should_uploadExistingFileToDirectory() {
        val file = File("src/test/resources/files/photo.png");
        val mock = MockMultipartFile("photo.png", file.inputStream())
        val surveyId = UUID.randomUUID()

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, mock, "test.png")

        assertThat(fileSystemHelper.doesFileExists(surveyId, SurveyFolder.RESOURCES, "test.png")).isTrue()
    }

    @Test
    fun upload_should_createFileAndUploadToDirectory() {
        val surveyId = UUID.randomUUID()

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", "test.txt")

        assertThat(fileSystemHelper.doesFileExists(surveyId, SurveyFolder.RESOURCES, "test.txt")).isTrue()
        assertThat(fileSystemHelper.getText(surveyId, SurveyFolder.RESOURCES, "test.txt")).isEqualTo("TEST123")
    }

    @Test
    fun listSurveyResources_should_listAllFilesFromResourcesFolder() {
        val surveyId = UUID.randomUUID()

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", "test.txt")

        val resources = fileSystemHelper.listSurveyResources(surveyId)

        assertThat(resources).hasSize(1)
            .extracting(FileInfo::name).containsOnly(tuple("test.txt"))
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

        fileSystemHelper.upload(surveyId, SurveyFolder.RESOURCES, "TEST123", "test.txt")

        val resources = fileSystemHelper.filerSurveyResources(surveyId, files = listOf("test.txt"))

        assertThat(resources).hasSize(1)
            .extracting(FileInfo::name).containsOnly(tuple("test.txt"))
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


}