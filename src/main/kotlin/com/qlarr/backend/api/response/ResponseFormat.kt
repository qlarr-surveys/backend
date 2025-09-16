package com.qlarr.backend.api.response

enum class ResponseFormat {
    CSV, ODS, XLSX;

    fun contentType(): String = when (this) {
        CSV -> "text/csv"
        ODS -> "application/vnd.oasis.opendocument.spreadsheet"
        XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }

    companion object {
        fun fromString(input: String?) = ResponseFormat.entries.firstOrNull {
            it.name.lowercase() == input?.lowercase()
        } ?: ResponseFormat.CSV
    }
}


enum class ResponseStatus {
    ALL, COMPLETE, INCOMPLETE, PREVIEW;

    companion object {
        fun fromString(input: String?) = entries.firstOrNull {
            it.name.lowercase() == input?.lowercase()
        } ?: ALL
    }
}