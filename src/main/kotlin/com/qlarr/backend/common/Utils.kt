package com.qlarr.backend.common

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


@Component
class UserUtils {
    fun currentUserId(): UUID = UUID.fromString(SecurityContextHolder.getContext().authentication.principal as String)

    fun currentAuthToken(): String = SecurityContextHolder.getContext().authentication.credentials as String
}

fun nowUtc(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

fun stripTags(string: String?): String? {
    return string?.replace(Regex("<[^>]*>?"), "")
            ?.replace("\n", "")
            ?.replace("&nbsp;", "")
            ?: string
}

fun Map<String, String>.stripHtmlTags(): Map<String, String> {
    val mutableMap = toMutableMap()
    for ((key, value) in mutableMap) {
        mutableMap[key] = stripTags(value).orEmpty()
    }
    return mutableMap
}


fun String.isValidName() = trim().length in 1..50

fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex(RegexOption.IGNORE_CASE)
    return matches(emailRegex)
}


