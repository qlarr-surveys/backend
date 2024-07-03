package com.frankie.backend.security

import com.frankie.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.frankie.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import com.frankie.backend.services.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Service

@Service
class LogoutServiceHandler(
        private val userService: UserService
) : LogoutHandler {
    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        val authenticationHeader = request.getHeader(HEADER_STRING)
        if (authenticationHeader != null && authenticationHeader.startsWith(TOKEN_PREFIX)) {
            val token = authenticationHeader.substring(7)
            userService.invalidateRefreshToken(token)
            SecurityContextHolder.clearContext()
            request.logout()
        }
        return
    }
}
