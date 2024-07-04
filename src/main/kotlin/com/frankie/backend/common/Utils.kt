package com.frankie.backend.common

import com.frankie.backend.api.user.Roles
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


@Component
class UserUtils {
    fun currentUserId(): UUID = UUID.fromString(SecurityContextHolder.getContext().authentication.principal as String)

    fun isSuperAdmin() = SecurityContextHolder.getContext().authentication.authorities
            .any { it.authority == Roles.SUPER_ADMIN.name.lowercase() }


    fun currentAuthToken(): String = SecurityContextHolder.getContext().authentication.credentials as String
}

fun nowUtc() = LocalDateTime.now(ZoneOffset.UTC)

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

fun getClientIp(request: HttpServletRequest): String {
    var clientIp = request.getHeader("X-Forwarded-For")
    if (clientIp == null || clientIp.isEmpty() || "unknown".equals(clientIp, ignoreCase = true)) {
        clientIp = request.getHeader("Proxy-Client-IP")
    }
    if (clientIp == null || clientIp.isEmpty() || "unknown".equals(clientIp, ignoreCase = true)) {
        clientIp = request.getHeader("WL-Proxy-Client-IP")
    }
    if (clientIp == null || clientIp.isEmpty() || "unknown".equals(clientIp, ignoreCase = true)) {
        clientIp = request.getHeader("HTTP_CLIENT_IP")
    }
    if (clientIp == null || clientIp.isEmpty() || "unknown".equals(clientIp, ignoreCase = true)) {
        clientIp = request.getHeader("HTTP_X_FORWARDED_FOR")
    }
    if (clientIp == null || clientIp.isEmpty() || "unknown".equals(clientIp, ignoreCase = true)) {
        clientIp = request.remoteAddr
    }
    return clientIp ?: ""
}

fun String.isValidName() = trim().length in 1..50

fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex(RegexOption.IGNORE_CASE)
    return matches(emailRegex)
}


