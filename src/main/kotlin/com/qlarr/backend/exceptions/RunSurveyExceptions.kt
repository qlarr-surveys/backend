package com.qlarr.backend.exceptions

import java.time.LocalDateTime

class SurveyNotStartedException(val startDate: LocalDateTime) : Throwable()
class SurveyExpiredException : Throwable()
class SurveyQuotaExceeded : Throwable()
class ClientTimeSkewException(val clientTime: LocalDateTime, val serverTime: LocalDateTime) : Throwable()