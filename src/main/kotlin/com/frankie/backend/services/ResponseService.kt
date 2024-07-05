package com.frankie.backend.services

import com.amazonaws.services.s3.Headers
import com.frankie.backend.api.response.ResponseDto
import com.frankie.backend.api.response.ResponseUploadFile
import com.frankie.backend.api.response.ResponsesDto
import com.frankie.backend.api.response.UploadResponseRequestData
import com.frankie.backend.api.survey.Status
import com.frankie.backend.common.SurveyFolder
import com.frankie.backend.common.stripHtmlTags
import com.frankie.backend.exceptions.*
import com.frankie.backend.expressionmanager.SurveyProcessor
import com.frankie.backend.helpers.S3Helper
import com.frankie.backend.mappers.ResponseMapper
import com.frankie.backend.mappers.valueNames
import com.frankie.backend.persistence.entities.SurveyResponseEntity
import com.frankie.backend.persistence.repositories.ResponseRepository
import com.frankie.expressionmanager.ext.labels
import com.frankie.expressionmanager.ext.splitToComponentCodes
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.usecase.SurveyDesignWithErrorException
import com.frankie.expressionmanager.usecase.defaultLang
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.StringWriter
import java.nio.file.Files
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class ResponseService(
        private val responseRepository: ResponseRepository,
        private val surveyService: SurveyService,
        private val helper: S3Helper,
        private val designService: DesignService,
        private val responseMapper: ResponseMapper,
) {
    fun uploadResponseFile(
            serverName: String,
            surveyId: UUID,
            responseId: UUID,
            questionId: String,
            isPreview: Boolean,
            file: MultipartFile
    ): ResponseUploadFile {
        val survey = surveyService.getSurveyById(surveyId, bypassPermission = true)
        if (!isPreview && !survey.isActive()) {
            throw SurveyIsNotActiveException()
        }
        val response = responseRepository.findByIdOrNull(responseId) ?: throw ResponseNotFoundException()
        val fileName = UUID.randomUUID().toString()
        helper.upload(surveyId, SurveyFolder.RESPONSES, file, file.contentType ?: "", fileName)
        val responseUploadFile = ResponseUploadFile(
                file.originalFilename!!, fileName, file.size, file.contentType
                ?: ""
        )
        val newValues = response.values.toMutableMap()
                .apply { put("$questionId.value", jacksonKtMapper.convertValue(responseUploadFile, Map::class.java)) }
        responseRepository.save(response.copy(values = newValues))
        return responseUploadFile
    }

    fun uploadOfflineResponseFile(
            surveyId: UUID,
            filename: String,
            file: MultipartFile
    ): ResponseUploadFile {
        val survey = surveyService.getSurveyById(surveyId)
        if (survey.status != Status.ACTIVE) {
            throw SurveyIsNotActiveException()
        }
        val mimeType = file.originalFilename!!.let { Files.probeContentType(File(it).toPath()) }
        helper.upload(surveyId, SurveyFolder.RESPONSES, file, mimeType, filename)
        return ResponseUploadFile(filename, filename, file.size, file.contentType ?: "")
    }

    fun isOfflineFileAlreadyUploaded(
            surveyId: UUID,
            filename: String
    ): Boolean {
        return helper.doesFileExists(surveyId, filename)
    }

    fun uploadOfflineSurveyResponse(
            surveyId: UUID,
            responseId: UUID,
            uploadResponseRequestData: UploadResponseRequestData
    ) {
        val survey = designService.getProcessedSurveyByVersion(surveyId, uploadResponseRequestData.versionId)
        if (survey.survey.status != Status.ACTIVE) {
            throw SurveyIsNotActiveException()
        } else if (!survey.latestVersion.valid) {
            throw SurveyDesignWithErrorException
        } else if (responseRepository.existsById(responseId)) {
            throw ResponseAlreadySyncedException()
        }

        if (uploadResponseRequestData.navigationIndex !is NavigationIndex.End) {
            throw IncompleteResponse()
        }

        val navigationUseCaseInput = NavigationUseCaseInput(
                values = uploadResponseRequestData.values,
                navigationInfo = NavigationInfo(
                        navigationDirection = NavigationDirection.Resume,
                        navigationIndex = uploadResponseRequestData.navigationIndex
                ),
                lang = uploadResponseRequestData.lang,
        )
        val navigationResult =
                SurveyProcessor.navigate(
                        survey.validationJsonOutput,
                        navigationUseCaseInput,
                        false,
                        SurveyMode.OFFLINE
                )

        val isValid: Boolean =
                navigationResult.state["frankieVariables"]?.get("Survey")?.get("validity")?.booleanValue() ?: false
        if (!isValid) {
            throw InvalidResponse()
        }

        val responseEntity = SurveyResponseEntity(
                id = responseId,
                version = uploadResponseRequestData.versionId,
                startDate = uploadResponseRequestData.startDate,
                surveyId = surveyId,
                surveyor = UUID.fromString(uploadResponseRequestData.userId),
                lang = uploadResponseRequestData.lang,
                navigationIndex = uploadResponseRequestData.navigationIndex,
                submitDate = uploadResponseRequestData.submitDate,
                values = uploadResponseRequestData.values
        )
        responseRepository.save(responseEntity)
    }


    fun downloadFile(
            serverName: String,
            surveyId: UUID,
            filename: UUID
    ): ResponseEntity<InputStreamResource> {
        val file = helper.download(surveyId, SurveyFolder.RESPONSES, filename.toString())
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .header(Headers.CONTENT_TYPE, file.objectMetadata.userMetadata["content-Type"]!!)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .eTag(file.objectMetadata.eTag) // lastModified is also ava
                .body(InputStreamResource(file.stream))
    }

    private fun getResponsesPage(
            surveyId: UUID,
            complete: Boolean?,
            surveyor: UUID?,
            usePagination: Boolean = true,
            page: Int? = null,
            perPage: Int? = null,
    ): Page<ResponseWithSurveyorName> {
        val pageable = if (!usePagination)
            Pageable.unpaged()
        else
            Pageable.ofSize(perPage ?: PER_PAGE).withPage((page ?: PAGE) - 1)
        return when {
            surveyor != null -> responseRepository.findAllBySurveyIdAndSurveyor(surveyId, surveyor, pageable)
            complete == null -> responseRepository.findAllBySurveyId(surveyId, pageable)

            complete == true -> responseRepository.findAllBySurveyIdAndSubmitDateIsNotNull(
                    surveyId,
                    pageable
            )

            complete == false -> responseRepository.findAllBySurveyIdAndSubmitDateIsNull(
                    surveyId,
                    pageable
            )

            else -> throw IllegalStateException("should not be here")
        }
    }

    fun getAllResponses(
            surveyId: UUID,
            page: Int?,
            perPage: Int?,
            complete: Boolean?,
            surveyor: UUID?
    ): ResponsesDto {
        val responses = getResponsesPage(surveyId, complete, surveyor, true, page, perPage)
        if (responses.isEmpty)
            return ResponsesDto(0, 0, 0, emptyList(), emptyList())
        val colNames = responses.map { it.response.values }.toList().valueNames()
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            responseMapper.toDto(responseEntity, colNames)
        }
        return ResponsesDto(
                responses.totalElements.toInt(),
                responses.totalPages,
                responses.pageable.pageNumber + 1,
                colNames,
                values
        )
    }

    fun exportResponses(
            surveyId: UUID,
            complete: Boolean?,
            clientZoneId: ZoneId
    ): ByteArray {
        val responses = getResponsesPage(surveyId, complete, null, false)
        if (responses.isEmpty)
            return ByteArray(0)
        val colNames = ADDITIONAL_COL_NAMES.toMutableList().apply {
            addAll(responses.map { it.response.values }.toList().valueNames())
        }
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            responseMapper.toDto(entity = responseEntity, valueNames = colNames, clientZoneId = clientZoneId)
        }
        val sw = StringWriter()
        val csvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
                .setHeader(*colNames.toTypedArray())
                .build()

        val printer = CSVPrinter(sw, csvFormat)
        values.forEach {
            mutableListOf<Any?>(
                    it.id,
                    it.preview,
                    it.version,
                    it.startDate,
                    it.submitDate,
                    it.lang
            ).apply {
                addAll(it.values)
            }.let { list ->
                printer.printRecord(list)
            }
        }
        return sw.buffer.toString().toByteArray()
    }

    fun getAllTextResponses(
            surveyId: UUID,
            page: Int?,
            perPage: Int?,
            complete: Boolean?,
            surveyor: UUID?
    ): ResponsesDto {
        val responses = getResponsesPage(surveyId, complete, surveyor, true, page, perPage)
        if (responses.isEmpty)
            return ResponsesDto(0, 0, 0, emptyList(), emptyList())
        val versions = responses.map { it.response.version }.distinct().map {
            designService.getProcessedSurveyByVersion(surveyId, it)
        }
        val labels = versions.last().run {
            validationJsonOutput.survey.labels(lang = validationJsonOutput.survey.defaultLang())
                    .filterValues { it.isNotEmpty() }
        }
        val componentsByOrder = versions.last().run {
            validationJsonOutput.componentIndexList.map { it.code }
        }
        val valueNames = responses.map { it.response.values }.toList().valueNames().sortedBy {
            componentsByOrder.indexOf(it.split(".")[0])
        }
        val colNames = valueNames.map {
            val names = it.split(".")
            val componentCode = names[0]
            if (componentCode.splitToComponentCodes().size > 1) {
                (labels[componentCode.splitToComponentCodes()[0]] ?: componentCode.splitToComponentCodes()[0]) +
                        " [${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else " [${names[1]}]"}]"
            } else {
                "${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else " [${names[1]}]"}"
            }
        }
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            val version = versions.first { it.latestVersion.version == responseEntity.response.version }
            val maskedValues = SurveyProcessor.maskedValues(
                    version.validationJsonOutput, NavigationUseCaseInput(
                    values = responseEntity.response.values
            )
            )
            responseMapper.toDto(responseEntity, valueNames, maskedValues)

        }
        return ResponsesDto(
                responses.totalElements.toInt(),
                responses.totalPages,
                responses.pageable.pageNumber + 1,
                colNames,
                values
        )
    }


    fun exportTextResponses(
            surveyId: UUID,
            complete: Boolean?,
            clientZoneId: ZoneId,
    ): ByteArray {
        val responses = getResponsesPage(surveyId, complete, null, false)
        if (responses.isEmpty)
            return ByteArray(0)
        val versions = responses.map { it.response.version }.distinct().map {
            designService.getProcessedSurveyByVersion(surveyId, it)
        }
        val labels = versions.last().run {
            validationJsonOutput.survey.labels(lang = validationJsonOutput.survey.defaultLang())
                    .filterValues { it.isNotEmpty() }
                    .stripHtmlTags()
        }
        val componentsByOrder = versions.last().run {
            validationJsonOutput.componentIndexList.map { it.code }
        }
        val valueNames = responses.map { it.response.values }.toList().valueNames().sortedBy {
            componentsByOrder.indexOf(it.split(".")[0])
        }
        val colNames = valueNames.map {
            val names = it.split(".")
            val componentCode = names[0]
            if (componentCode.splitToComponentCodes().size > 1) {
                (labels[componentCode.splitToComponentCodes()[0]] ?: componentCode.splitToComponentCodes()[0]) +
                        "[${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else "[${names[1]}]"}]"
            } else {
                "${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else "[${names[1]}]"}"
            }
        }

        val finalColNames = ADDITIONAL_COL_NAMES.toMutableList().apply {
            addAll(colNames)
        }
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            val version = versions.first { it.latestVersion.version == responseEntity.response.version }
            val maskedValues = SurveyProcessor.maskedValues(
                    version.validationJsonOutput, NavigationUseCaseInput(
                    values = responseEntity.response.values
            )
            )
            responseMapper.toDto(responseEntity, valueNames, maskedValues, clientZoneId)

        }
        val sw = StringWriter()
        val csvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
                .setHeader(*finalColNames.toTypedArray())
                .build()

        val printer = CSVPrinter(sw, csvFormat)
        values.forEach {
            mutableListOf<Any?>(
                    it.id,
                    it.preview,
                    it.version,
                    it.startDate,
                    it.submitDate,
                    it.lang
            ).apply {
                addAll(it.values)
            }.let { list ->
                printer.printRecord(list)
            }
        }
        return sw.buffer.toString().toByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    fun deleteResponse(surveyId: UUID, responseId: UUID) {
        val response = responseRepository.findByIdOrNull(responseId) ?: throw ResponseNotFoundException()
        val processedSurvey = designService.getProcessedSurveyByVersion(surveyId, response.version)
        processedSurvey.validationJsonOutput.schema
                .filter { it.dataType == DataType.FILE }
                .forEach { responseField ->
                    response.values[responseField.toValueKey()]?.let {
                        (it as Map<String, String>)["stored_filename"]?.let { storedFileName ->
                            helper.delete(surveyId, SurveyFolder.RESPONSES, storedFileName)
                        }
                    }

                }
        responseRepository.delete(response)
    }

    companion object {
        const val PER_PAGE = 10
        const val PAGE = 1

        val ADDITIONAL_COL_NAMES = listOf("id", "preview", "version", "ip", "start_date", "submit_date", "Lang")
    }

}

interface ResponseWithSurveyorName {
    val response: SurveyResponseEntity
    val firstName: String?
    val lastName: String?
}