package com.qlarr.backend.common

import java.time.LocalDateTime
import java.time.ZoneId


const val DATE_TIME_UTC_FORMAT = "yyyy-MM-dd HH:mm:ss"
const val RECENT_LOGIN_SPAN = 10 * 60 * 1000

sealed class SurveyFolder(val path: String) {
    data class Responses(private val responseId: String) : SurveyFolder("responses/${responseId}")
    data object Resources : SurveyFolder("resources")
    data object Design : SurveyFolder("design")
}


fun LocalDateTime.utcToLocalTZ(zoneId: ZoneId?): LocalDateTime {
    return if (zoneId == null) {
        this
    } else {
        val utcZonedDateTime = this.atZone(ZoneId.of("UTC"))
        // Convert the ZonedDateTime in UTC to the specified ZoneId
        val zonedDateTimeInTargetZone = utcZonedDateTime.withZoneSameInstant(zoneId)
        // Convert back to LocalDateTime
        zonedDateTimeInTargetZone.toLocalDateTime()
    }
}

