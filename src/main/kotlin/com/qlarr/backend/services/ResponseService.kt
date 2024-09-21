package com.qlarr.backend.services

import com.qlarr.backend.api.response.ResponseDto
import com.qlarr.backend.api.response.ResponsesDto
import com.qlarr.backend.common.stripHtmlTags
import com.qlarr.backend.expressionmanager.SurveyProcessor
import com.qlarr.backend.mappers.ResponseMapper
import com.qlarr.backend.mappers.valueNames
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.expressionmanager.ext.labels
import com.qlarr.expressionmanager.ext.splitToComponentCodes
import com.qlarr.expressionmanager.model.NavigationUseCaseInput
import com.qlarr.expressionmanager.model.ReservedCode
import com.qlarr.expressionmanager.usecase.defaultLang
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.time.ZoneId
import java.util.*


@Service
class ResponseService(
        private val responseRepository: ResponseRepository,
        private val designService: DesignService,
        private val responseMapper: ResponseMapper,
) {
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

    companion object {
        const val PER_PAGE = 10
        const val PAGE = 1

        val ADDITIONAL_COL_NAMES = listOf("id", "preview", "version", "start_date", "submit_date", "Lang")
    }

}

interface ResponseWithSurveyorName {
    val response: SurveyResponseEntity
    val firstName: String?
    val lastName: String?
}