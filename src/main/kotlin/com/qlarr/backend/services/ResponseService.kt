package com.qlarr.backend.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
import com.qlarr.surveyengine.model.Dependency
import com.qlarr.surveyengine.model.ReservedCode
import com.qlarr.surveyengine.model.ReservedCode.Order
import com.qlarr.surveyengine.model.exposed.ReturnType
import com.qlarr.surveyengine.model.sortChildren
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
        values: List<List<Any?>>,
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

            response.forEachIndexed { colIndex, value ->
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
        values: List<List<Any?>>,
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
            response.forEachIndexed { colIndex, value ->
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
        values: List<List<Any?>>,
        colNames: List<String>
    ): ByteArray {
        val sw = StringWriter()
        val csvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
            .setHeader(*colNames.toTypedArray())
            .build()

        CSVPrinter(sw, csvFormat).use { printer ->
            values.forEach { row ->
                printer.printRecord(row)
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
        val colNames: List<String> = processed.validationJsonOutput.schema.map { it.toValueKey() }
        val finalColNames = ADDITIONAL_COL_NAMES + colNames
        val values: List<List<Any?>> = responses.toList().map { responseEntity ->
            mutableListOf(
                responseEntity.response.surveyResponseIndex,
                responseEntity.response.id,
                responseEntity.response.startDate,
                responseEntity.response.submitDate,
                responseEntity.response.lang,
                responseEntity.response.values["Survey.disqualified"] ?: false
            ).apply {
                addAll(colNames.map { responseEntity.response.values[it] })
            }
        }
        return when (responseFormat) {
            ResponseFormat.CSV -> exportCsv(values, finalColNames)
            ResponseFormat.ODS -> exportOds(values, finalColNames)
            ResponseFormat.XLSX -> exportXlsx(values, finalColNames)
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
        val indexList = processed.validationJsonOutput.buildCodeIndex()
        val labels = processed.validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()

        val componentsByOrder = processed.validationJsonOutput.componentIndexList.map { it.code }
        val valueNames = responses.map { it.response.values }.toList().valueNames().sortedBy {
            componentsByOrder.indexOf(it.split(".")[0])
        }
        val colNames = valueNames.map {
            val names = it.split(".")
            val componentCode = names[0]
            val instructionCode = names[1]
            val componentCodes = componentCode.splitToComponentCodes()
            // this is an answer, we could add the question code
            if (componentCodes.size > 1) {
                val questionCode = componentCodes[0]
                "(${indexList[questionCode]}) ${labels[questionCode] ?: ""}" + " - " + (labels[componentCode]
                    ?: componentCode)
            } else {
                "(${indexList[componentCode]}) ${labels[componentCode] ?: ""}"
            } + if (instructionCode == ReservedCode.Value.code) "" else "[${instructionCode}]"
        }

        val finalColNames = ADDITIONAL_COL_NAMES.toMutableList().apply {
            addAll(colNames)
        }
        val values: List<List<Any?>> = responses.toList().map { responseEntity ->
            val maskedValues = SurveyProcessor.maskedValues(
                values = responseEntity.response.values
            )
            mutableListOf(
                responseEntity.response.surveyResponseIndex,
                responseEntity.response.id,
                responseEntity.response.startDate,
                responseEntity.response.submitDate,
                responseEntity.response.lang,
                responseEntity.response.values["Survey.disqualified"] ?: false
            ).apply {
                addAll(valueNames.map { valueKey->
                    val names = valueKey.split(".")
                        maskedValues["${names[0]}.${ReservedCode.MaskedValue.code}"]?.let {
                            "$it [${responseEntity.response.values[valueKey]}]"
                        } ?: responseEntity.response.values[valueKey]
                })
            }
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
        confirmFilesExport: Boolean,
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
            values,
            canExportFiles = if (confirmFilesExport) {
                designService.getLatestProcessedSurvey(surveyId).validationJsonOutput.schema.any {
                    it.dataType == ReturnType.File
                }
            } else false
        )

    }

    fun getResponse(responseId: UUID): ResponseDto {
        val response = responseRepository.findByIdOrNull(responseId) ?: throw Exception()
        val processed = designService.getLatestProcessedSurvey(response.surveyId)
        val indexList = processed.validationJsonOutput.buildCodeIndex()
        val componentIndexList = processed.validationJsonOutput.componentIndexList
            .toMutableList()
            .sortChildren(response.values)
        val labels = processed.validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
        val maskedValues = SurveyProcessor.maskedValues(
            values = response.values
        )
        val values = response.values
            .filterKeys { it.split(".").last() == "value" }
            .toSortedMap { key1: String, key2: String ->
                val code1 = key1.split(".")[0]  // Extract component code
                val code2 = key2.split(".")[0]  // Extract component code
                componentIndexList.indexOfFirst {
                    it.code == code1
                } - componentIndexList.indexOfFirst {
                    it.code == code2
                }
            }
            .mapValues { entry ->
                maskedValues[entry.key.split(".")[0] + ".masked_value"]?.let {
                    "$it (${entry.value})"
                } ?: entry.value
            }.mapKeys { entry ->
                val names = entry.key.split(".")
                val componentCode = names[0]
                val instructionCode = names[1]
                val componentCodes = componentCode.splitToComponentCodes()
                // this is an answer, we could add the question code
                if (componentCodes.size > 1) {
                    val questionCode = componentCodes[0]
                    "(${indexList[questionCode]}) ${labels[questionCode] ?: ""}" + " - " + (labels[componentCode]
                        ?: componentCode)
                } else {
                    "(${indexList[componentCode]}) ${labels[componentCode] ?: ""}"
                } + if (instructionCode == ReservedCode.Value.code) "" else "[${instructionCode}]"
            }

        return responseMapper.toDto(
            disqualified = response.values["Survey.disqualified"] as? Boolean ?: false,
            entity = response,
            values = values
        )
    }

    companion object {
        const val PER_PAGE = 10
        const val PAGE = 1

        private val ADDITIONAL_COL_NAMES = listOf("index", "id", "start_date", "submit_date", "Lang", "disqualified")
    }

}

interface ResponseWithSurveyorName {
    val response: SurveyResponseEntity
    val firstName: String?
    val lastName: String?
}