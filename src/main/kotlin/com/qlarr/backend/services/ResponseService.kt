package com.qlarr.backend.services

import com.qlarr.backend.api.response.ResponseDto
import com.qlarr.backend.api.response.ResponsesDto
import com.qlarr.backend.common.stripHtmlTags
import com.qlarr.backend.expressionmanager.SurveyProcessor
import com.qlarr.backend.helpers.FileHelper
import com.qlarr.backend.mappers.ResponseMapper
import com.qlarr.backend.mappers.valueNames
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


@Service
class ResponseService(
    private val responseRepository: ResponseRepository,
    private val designService: DesignService,
    private val responseMapper: ResponseMapper,
    private val fileHelper: FileHelper,
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
            validationJsonOutput.labels().filterValues { it.isNotEmpty() }
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
                validationJsonOutput = version.validationJsonOutput,
                values = responseEntity.response.values,
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
            validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
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
                version.validationJsonOutput,
                values = responseEntity.response.values

            )
            responseMapper.toDto(responseEntity, valueNames, maskedValues, clientZoneId)

        }
        val sw = StringWriter()
        val csvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
            .setHeader(*finalColNames.toTypedArray())
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

    fun exportResponsesXlsx(
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

    fun exportTextResponsesXlsx(
        surveyId: UUID,
        complete: Boolean?,
        clientZoneId: ZoneId
    ): ByteArray {
        val responses = getResponsesPage(surveyId, complete, null, false)
        if (responses.isEmpty)
            return ByteArray(0)
        val versions = responses.map { it.response.version }.distinct().map {
            designService.getProcessedSurveyByVersion(surveyId, it)
        }
        val labels = versions.last().run {
            validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
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
                "${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else "[${names[1]}]"}]"
            }
        }

        val finalColNames = ADDITIONAL_COL_NAMES.toMutableList().apply {
            addAll(colNames)
        }
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            val version = versions.first { it.latestVersion.version == responseEntity.response.version }
            val maskedValues = SurveyProcessor.maskedValues(
                version.validationJsonOutput,
                values = responseEntity.response.values
            )
            responseMapper.toDto(responseEntity, valueNames, maskedValues, clientZoneId)
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Responses")

        val headerRow = sheet.createRow(0)
        finalColNames.forEachIndexed { index, colName ->
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

        finalColNames.indices.forEach { sheet.autoSizeColumn(it) }

        return workbook.use { wb ->
            ByteArrayOutputStream().use { outputStream ->
                wb.write(outputStream)
                outputStream.toByteArray()
            }.takeIf { it.isNotEmpty() }
                ?: throw RuntimeException("Generated Excel file is empty")
        }
    }

    fun exportResponsesOdf(
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

    fun exportTextResponsesOdf(
        surveyId: UUID,
        complete: Boolean?,
        clientZoneId: ZoneId
    ): ByteArray {
        val responses = getResponsesPage(surveyId, complete, null, false)
        if (responses.isEmpty)
            return ByteArray(0)
        val versions = responses.map { it.response.version }.distinct().map {
            designService.getProcessedSurveyByVersion(surveyId, it)
        }
        val labels = versions.last().run {
            validationJsonOutput.labels().filterValues { it.isNotEmpty() }.stripHtmlTags()
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
                "${labels[names[0]] ?: names[0]}${if (names[1] == ReservedCode.Value.code) "" else "[${names[1]}]"}]"
            }
        }

        val finalColNames = ADDITIONAL_COL_NAMES.toMutableList().apply {
            addAll(colNames)
        }
        val values: List<ResponseDto> = responses.toList().map { responseEntity ->
            val version = versions.first { it.latestVersion.version == responseEntity.response.version }
            val maskedValues = SurveyProcessor.maskedValues(
                version.validationJsonOutput,
                values = responseEntity.response.values
            )
            responseMapper.toDto(responseEntity, valueNames, maskedValues, clientZoneId)
        }

        val document = OdfSpreadsheetDocument.newSpreadsheetDocument()
        val table = document.spreadsheetTables.first()

        val headerRow = table.getRowByIndex(0)
        finalColNames.forEachIndexed { index, colName ->
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

    fun bulkDownloadResponses(
        surveyId: UUID,
    ): ResponseEntity<InputStreamResource> {
        val responses = getResponsesPage(surveyId, true, null, false)
        if (responses.isEmpty) {
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