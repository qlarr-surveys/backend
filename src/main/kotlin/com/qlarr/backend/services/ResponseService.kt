package com.qlarr.backend.services

import com.qlarr.backend.api.response.*
import com.qlarr.backend.common.SurveyFolder
import com.qlarr.backend.common.stripHtmlTags
import com.qlarr.backend.exceptions.SizeLimitExceededException
import com.qlarr.backend.expressionmanager.SurveyProcessor
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.mappers.ResponseMapper
import com.qlarr.backend.mappers.valueNames
import com.qlarr.backend.persistence.entities.ResponseSummaryInterface
import com.qlarr.backend.persistence.entities.SurveyResponseEntity
import com.qlarr.backend.persistence.repositories.ResponseRepository
import com.qlarr.surveyengine.ext.splitToComponentCodes
import com.qlarr.surveyengine.model.ReservedCode
import com.qlarr.surveyengine.model.exposed.ReturnType
import com.qlarr.surveyengine.model.sortChildren
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
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

    private val logger = LoggerFactory.getLogger(ResponseService::class.java)
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
                addAll(valueNames.map { valueKey ->
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

    private data class FileToDownload(
        val responseId: UUID,
        val surveyResponseIndex: Int,
        val questionId: String,
        val storedFilename: String,
        val originalFilename: String,
        val size: Long
    )

    fun bulkDownloadResponses(
        surveyId: UUID,
        complete: Boolean?,
        from: Int,
        to: Int
    ): ResponseEntity<StreamingResponseBody> {
        val responses = getResponsesFromTo(surveyId, complete, from, to)
        if (responses.isEmpty()) {
            return ResponseEntity.noContent().build()
        }

        // Collect all files to download with their metadata
        val filesToDownload = mutableListOf<FileToDownload>()
        var totalSizeBytes = 0L

        responses.forEach { responseWithSurveyor ->
            val response = responseWithSurveyor.response
            response.values.forEach { (questionId, value) ->
                if (value is Map<*, *> && value.containsKey("stored_filename")) {
                    val storedFilename = value["stored_filename"] as? String
                    val originalFilename = value["filename"] as? String
                    val size = (value["size"] as? Number)?.toLong()

                    if (storedFilename != null && originalFilename != null && size != null) {
                        filesToDownload.add(
                            FileToDownload(
                                responseId = response.id,
                                surveyResponseIndex = response.surveyResponseIndex!!,
                                questionId = questionId,
                                storedFilename = storedFilename,
                                originalFilename = originalFilename,
                                size = size
                            )
                        )
                        totalSizeBytes += size
                    }
                }
            }
        }

        if (filesToDownload.isEmpty()) {
            return ResponseEntity.noContent().build()
        }

        // Check size limit BEFORE streaming starts
        val maxSizeBytes = 200 * 1024 * 1024L // 200MB
        if (totalSizeBytes > maxSizeBytes) {
            throw SizeLimitExceededException(
                "Total download size (${totalSizeBytes / 1024 / 1024}MB) exceeds limit of ${maxSizeBytes / 1024 / 1024}MB. " +
                        "Please reduce the range or contact support for larger exports."
            )
        }

        val streamingBody = StreamingResponseBody { outputStream ->
            ZipOutputStream(outputStream).use { zip ->
                filesToDownload.forEach { fileInfo ->
                    try {
                        fileHelper.download(
                            surveyId,
                            SurveyFolder.Responses(fileInfo.responseId.toString()),
                            fileInfo.storedFilename
                        ).inputStream.use { inputStream ->
                            val zipEntryName =
                                "${fileInfo.surveyResponseIndex}-${fileInfo.questionId}-${fileInfo.originalFilename}"
                            val entry = ZipEntry(zipEntryName)
                            zip.putNextEntry(entry)

                            inputStream.copyTo(zip)

                            zip.closeEntry()
                        }
                    } catch (e: Exception) {
                        logger.error(
                            "Error downloading file ${fileInfo.storedFilename} for response ${fileInfo.responseId}",
                            e
                        )
                    }
                }
            }
        }

        return ResponseEntity.ok()
            .header(CONTENT_TYPE, "application/zip")
            .header("Content-Disposition", "attachment; filename=\"$surveyId-responses-files.zip\"")
            .body(streamingBody)
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
        val responseWithSurveyorName = responseRepository.responseWithSurveyorName(responseId) ?: throw Exception()
        val response = responseWithSurveyorName.response
        val processed = designService.getLatestProcessedSurvey(response.surveyId)
        val indexList = processed.validationJsonOutput.buildCodeIndex()
        val componentIndexList = processed.validationJsonOutput.componentIndexList
            .toMutableList()
            .sortChildren(response.values)
        val labels = processed.validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
        val maskedValues = SurveyProcessor.maskedValues(
            values = response.values
        )
        val componentEvents = response.events.filter {
            it.componentCode != null
        }

        val eventCodes = componentEvents.mapNotNull {
            it.componentCode
        }
        val valueCodes = response.values
            .filterKeys { it.split(".").last() == "value" }
            .map {
                it.key.split(".").first()
            }
        val values: List<ResponseValue> = componentIndexList
            .map { it.code }
            .filter {
                valueCodes.contains(it) || eventCodes.contains(it)
            }.map { code ->
                val componentCodes = code.splitToComponentCodes()
                val key = if (componentCodes.size > 1) {
                    val questionCode = componentCodes[0]
                    "(${indexList[questionCode]}) ${labels[questionCode] ?: ""}" + " - " + (labels[code]
                        ?: code)
                } else {
                    "(${indexList[code]}) ${labels[code] ?: ""}"
                }
                ResponseValue(
                    key = key,
                    code = code,
                    value = if (response.values.containsKey("$code.value")) {
                        val value = response.values["$code.value"]!!
                        maskedValues["$code.masked_value"]?.let { "$it ($value)" } ?: value
                    } else null,
                )
            }

        return responseMapper.toDto(
            surveyorName = response.surveyor?.let {
                "${responseWithSurveyorName.firstName} ${responseWithSurveyorName.lastName}"
            },
            disqualified = response.values["Survey.disqualified"] as? Boolean ?: false,
            entity = response,
            values = values,
            events = response.events.filter {
                it !is ResponseEvent.Value && it !is ResponseEvent.Navigation
            }
        )
    }

    fun getResponseWithEvents(responseId: UUID): ResponseWithEventsDto {
        val responseWithSurveyorName = responseRepository.responseWithSurveyorName(responseId) ?: throw Exception()
        val response = responseWithSurveyorName.response
        val processed = designService.getLatestProcessedSurvey(response.surveyId)
        val indexList = processed.validationJsonOutput.buildCodeIndex()
        val componentIndexList = processed.validationJsonOutput.componentIndexList
            .toMutableList()
            .sortChildren(response.values)
        val labels = processed.validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
        val maskedValues = SurveyProcessor.maskedValues(
            values = response.values
        )
        val componentEvents = response.events.filter {
            it.componentCode != null
        }

        val eventCodes = componentEvents.mapNotNull {
            it.componentCode
        }
        val valueCodes = response.values
            .filterKeys { it.split(".").last() == "value" }
            .map {
                it.key.split(".").first()
            }
        val values: List<ResponseValue> = componentIndexList
            .map { it.code }
            .filter {
                valueCodes.contains(it) || eventCodes.contains(it)
            }.map { code ->
                val componentCodes = code.splitToComponentCodes()
                val key = if (componentCodes.size > 1) {
                    val questionCode = componentCodes[0]
                    "(${indexList[questionCode]}) ${labels[questionCode] ?: ""}" + " - " + (labels[code]
                        ?: code)
                } else {
                    "(${indexList[code]}) ${labels[code] ?: ""}"
                }
                ResponseValue(
                    key = key,
                    code = code,
                    value = if (response.values.containsKey("$code.value")) {
                        val value = response.values["$code.value"]!!
                        maskedValues["$code.masked_value"]?.let { "$it ($value)" } ?: value
                    } else null)

            }

        return responseMapper.toEventDto(
            surveyorName = response.surveyor?.let {
                "${responseWithSurveyorName.firstName} ${responseWithSurveyorName.lastName}"
            },
            disqualified = response.values["Survey.disqualified"] as? Boolean ?: false,
            entity = response,
            values = values,
            events = response.events.map { event->
                if (event is ResponseEvent.Value) {
                    val code = event.code
                    val componentCodes = code.splitToComponentCodes()
                    val key = if (componentCodes.size > 1) {
                        val questionCode = componentCodes[0]
                        "(${indexList[questionCode]}) ${labels[questionCode] ?: ""}" + " - " + (labels[code]
                            ?: code)
                    } else {
                        "(${indexList[code]}) ${labels[code] ?: ""}"
                    }
                    ResponseEventDto(
                        event,
                        responseValue = ResponseValue(
                            key = key,
                            code = code,
                            value = if (response.values.containsKey("$code.value")) {
                                val value = response.values["$code.value"]!!
                                maskedValues["$code.masked_value"]?.let { "$it ($value)" } ?: value
                            } else null)
                    )
                } else {
                    ResponseEventDto(
                        event,
                        responseValue = null
                    )
                }
            }
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

