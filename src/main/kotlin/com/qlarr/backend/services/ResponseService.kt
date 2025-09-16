package com.qlarr.backend.services

import com.qlarr.backend.api.response.*
import com.qlarr.backend.common.stripHtmlTags
import com.qlarr.backend.expressionmanager.SurveyProcessor
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.mappers.ResponseMapper
import com.qlarr.backend.mappers.valueNames
import com.qlarr.backend.persistence.entities.ResponseSummaryInterface
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.ext.splitToComponentCodes
import com.qlarr.surveyengine.model.ReservedCode
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders.CONTENT_LENGTH
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.time.ZoneId
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min


@Service
class ResponseService(
    private val responseRepository: ResponseRepository,
    private val designService: DesignService,
    private val responseMapper: ResponseMapper,
    private val fileHelper: FileHelper,
) {
    private fun getResponsesFromTo(
        surveyId: UUID,
        complete: Boolean?,
        from: Int,
        to: Int
    ): List<ResponseWithSurveyorName> {
        val lower = min(from, to)
        val upper = max(from, to)
        return when (complete) {
            null -> responseRepository.fromToBySurveyId(surveyId, lower, upper)
            true -> responseRepository.fromToBySurveyIdAndSubmitDateIsNotNull(
                surveyId,
                lower,
                upper
            )

            false -> responseRepository.fromToBySurveyIdAndSubmitDateIsNull(
                surveyId,
                lower,
                upper
            )
        }
    }


    private fun exportXlsx(
        values: List<ResponseDto>,
        colNames: List<String>
    ): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Responses")

        val headerRow = sheet.createRow(0)
        colNames.forEachIndexed { index, colName ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(colName)
        }

        values.forEachIndexed { rowIndex, response ->
            val row = sheet.createRow(rowIndex + 1)
            val rowData = mutableListOf<Any?>(
                response.id,
                response.preview,
                response.version,
                response.startDate,
                response.submitDate,
                response.lang
            ).apply {
                addAll(response.values.values)
            }

            rowData.forEachIndexed { colIndex, value ->
                val cell = row.createCell(colIndex)
                when (value) {
                    null -> cell.setCellValue("")
                    is String -> cell.setCellValue(value)
                    is Number -> cell.setCellValue(value.toDouble())
                    is Boolean -> cell.setCellValue(value)
                    else -> cell.setCellValue(value.toString())
                }
            }
        }

        colNames.indices.forEach { sheet.autoSizeColumn(it) }

        return workbook.use { wb ->
            ByteArrayOutputStream().use { outputStream ->
                wb.write(outputStream)
                outputStream.toByteArray()
            }.takeIf { it.isNotEmpty() }
                ?: throw RuntimeException("Generated Excel file is empty")
        }
    }

    private fun exportOds(
        values: List<ResponseDto>,
        colNames: List<String>
    ): ByteArray {
        val document = OdfSpreadsheetDocument.newSpreadsheetDocument()
        val table = document.spreadsheetTables.first()

        val headerRow = table.getRowByIndex(0)
        colNames.forEachIndexed { index, colName ->
            val cell = headerRow.getCellByIndex(index)
            cell.stringValue = colName
        }

        values.forEachIndexed { rowIndex, response ->
            val row = table.getRowByIndex(rowIndex + 1)
            val rowData = mutableListOf<Any?>(
                response.id,
                response.preview,
                response.version,
                response.startDate,
                response.submitDate,
                response.lang
            ).apply {
                addAll(response.values.values)
            }

            rowData.forEachIndexed { colIndex, value ->
                val cell = row.getCellByIndex(colIndex)
                when (value) {
                    null -> cell.stringValue = ""
                    is String -> cell.stringValue = value
                    is Number -> cell.doubleValue = value.toDouble()
                    is Boolean -> cell.booleanValue = value
                    else -> cell.stringValue = value.toString()
                }
            }
        }

        return document.use { doc ->
            ByteArrayOutputStream().use { outputStream ->
                doc.save(outputStream)
                outputStream.toByteArray()
            }.takeIf { it.isNotEmpty() }
                ?: throw RuntimeException("Generated ODF file is empty")
        }
    }

    private fun exportCsv(
        values: List<ResponseDto>,
        colNames: List<String>
    ): ByteArray {
        val sw = StringWriter()
        val csvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
            .setHeader(*colNames.toTypedArray())
            .build()

        CSVPrinter(sw, csvFormat).use { printer ->
            values.forEach {
                mutableListOf<Any?>(
                    it.id,
                    it.preview,
                    it.version,
                    it.startDate,
                    it.submitDate,
                    it.lang
                ).apply {
                    addAll(it.values.values)
                }.let { list ->
                    printer.printRecord(list)
                }
            }
        }
        return sw.buffer.toString().toByteArray()
    }


    fun exportResponses(
        surveyId: UUID,
        complete: Boolean?,
        clientZoneId: ZoneId,
        responseFormat: ResponseFormat,
        from: Int,
        to: Int
    ): ByteArray {
        val responses = getResponsesFromTo(surveyId, complete, from, to)
        if (responses.isEmpty())
            return ByteArray(0)
        val processed = designService.getLatestProcessedSurvey(surveyId)
        val colNames: List<String> = ADDITIONAL_COL_NAMES.toMutableList().apply {
            processed.validationJsonOutput.schema.map { it.toValueKey() }
        }
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            responseMapper.toDto(entity = responseEntity, valueNames = colNames, clientZoneId = clientZoneId)
        }
        return when (responseFormat) {
            ResponseFormat.CSV -> exportCsv(values, colNames)
            ResponseFormat.ODS -> exportOds(values, colNames)
            ResponseFormat.XLSX -> exportXlsx(values, colNames)
        }
    }


    fun exportTextResponses(
        surveyId: UUID,
        complete: Boolean?,
        clientZoneId: ZoneId,
        responseFormat: ResponseFormat,
        from: Int,
        to: Int
    ): ByteArray {
        val responses = getResponsesFromTo(surveyId, complete, from, to)
        if (responses.isEmpty())
            return ByteArray(0)
        val processed = designService.getLatestProcessedSurvey(surveyId)
        val labels = processed.validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()

        val componentsByOrder = processed.validationJsonOutput.componentIndexList.map { it.code }
        val valueNames = responses.map { it.response.values }.toList().valueNames().sortedBy {
            componentsByOrder.indexOf(it.split(".")[0])
        }
        val colNames = valueNames.map {
            val names = it.split(".")
            val componentCode = names[0]
            if (componentCode.splitToComponentCodes().size > 1) {
                (labels[componentCode.splitToComponentCodes()[0]] ?: componentCode.splitToComponentCodes()[0]) +
                        "(${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else "[${names[1]}]"})"
            } else {
                "${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else "[${names[1]}]"}"
            }
        }

        val finalColNames = ADDITIONAL_COL_NAMES.toMutableList().apply {
            addAll(colNames)
        }
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            val maskedValues = SurveyProcessor.maskedValues(
                values = responseEntity.response.values

            )
            responseMapper.toDto(responseEntity, valueNames, maskedValues, clientZoneId)

        }
        return when (responseFormat) {
            ResponseFormat.CSV -> exportCsv(values, finalColNames)
            ResponseFormat.ODS -> exportOds(values, finalColNames)
            ResponseFormat.XLSX -> exportXlsx(values, finalColNames)
        }

    }

    fun bulkDownloadResponses(
        surveyId: UUID,
        complete: Boolean?,
        from: Int,
        to: Int
    ): ResponseEntity<InputStreamResource> {
        val responses = getResponsesFromTo(surveyId, complete, from, to)
        if (responses.isEmpty()) {
            return ResponseEntity.noContent().build()
        }

        val zipBytes = ByteArrayOutputStream().use { zipStream ->
            ZipOutputStream(zipStream).use { zip ->

                responses.forEach { responseWithSurveyor ->
                    val response = responseWithSurveyor.response

                    response.values.forEach { (questionId, value) ->
                        if (value is Map<*, *> && value.containsKey("stored_filename")) {
                            val storedFilename = value["stored_filename"] as String
                            val originalFilename = value["filename"] as String

                            try {
                                val fileDownload = fileHelper.download(
                                    surveyId,
                                    com.qlarr.backend.common.SurveyFolder.Responses(response.id.toString()),
                                    storedFilename
                                )

                                val zipEntryName = "${response.surveyResponseIndex}-${questionId}-${originalFilename}"

                                val entry = ZipEntry(zipEntryName)
                                zip.putNextEntry(entry)

                                fileDownload.inputStream.use { inputStream ->
                                    inputStream.copyTo(zip)
                                }

                                zip.closeEntry()
                            } catch (e: Exception) {
                                println("Error downloading file $storedFilename for response ${response.id}: ${e.message}")
                            }
                        }
                    }
                }
            }
            zipStream.toByteArray()
        }

        return ResponseEntity.ok()
            .header(CONTENT_TYPE, "application/zip")
            .header("Content-Disposition", "attachment; filename=\"$surveyId-responses-files.zip\"")
            .header(CONTENT_LENGTH, zipBytes.size.toString())
            .body(InputStreamResource(ByteArrayInputStream(zipBytes)))
    }

    fun getSummary(
        surveyId: UUID,
        page: Int?,
        perPage: Int?,
        responseStatus: ResponseStatus,
        surveyor: UUID?,
    ): ResponsesSummaryDto {
        val pageable = Pageable.ofSize(perPage ?: PER_PAGE).withPage((page ?: PAGE) - 1)
        val responses: Page<ResponseSummaryInterface> = when {
            surveyor != null -> responseRepository.summaryBySurveyor(surveyId, surveyor, pageable)
            responseStatus == ResponseStatus.ALL -> responseRepository.summary(surveyId, pageable)
            responseStatus == ResponseStatus.PREVIEW -> responseRepository.previewSummary(surveyId, pageable)
            responseStatus == ResponseStatus.COMPLETE -> responseRepository.completeSummary(surveyId, pageable)
            responseStatus == ResponseStatus.INCOMPLETE -> responseRepository.incompleteSummary(surveyId, pageable)
            else -> throw IllegalStateException("should not be here")
        }
        val values: List<ResponseSummary> = responses.toList().map { toData(it) }
        return ResponsesSummaryDto(
            responses.totalElements.toInt(),
            responses.totalPages,
            responses.pageable.pageNumber + 1,
            values
        )

    }

    fun getResponse(responseId: UUID): ResponseDto {
        val response = responseRepository.findByIdOrNull(responseId) ?: throw Exception()
        val processed = designService.getLatestProcessedSurvey(response.surveyId)
        val labels = processed.validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
        val maskedValues = SurveyProcessor.maskedValues(
            values = response.values
        )
        val values = response.values
            .filterKeys { it.split(".")[1] == "value" }
            .mapValues { entry ->
                maskedValues[entry.key.split(".")[0] + ".masked_value"]?.let {
                    "$it (${entry.value})"
                } ?: entry.value
            }
            .mapKeys { entry ->
                val componentName = entry.key.split(".")[0]
                labels[componentName] ?: componentName
            }

        return responseMapper.toDto(response, values)
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