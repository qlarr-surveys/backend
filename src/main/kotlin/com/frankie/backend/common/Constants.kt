package com.frankie.backend.common

import java.time.LocalDateTime
import java.time.ZoneId


const val DATE_TIME_UTC_FORMAT = "yyyy-MM-dd HH:mm:ss"
const val CONFIRMATION_TOKEN_EXPIRY_DAYS = 1L
const val RECENT_LOGIN_SPAN = 10 * 60 * 1000

enum class SurveyFolder(val path: String) {
    RESPONSES("responses"),
    RESOURCES("resources"),
    DESIGN("design")
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

