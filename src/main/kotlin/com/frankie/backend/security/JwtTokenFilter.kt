package com.frankie.backend.security

import com.frankie.backend.security.constant.SecurityConstants.Companion.HEADER_STRING
import com.frankie.backend.security.constant.SecurityConstants.Companion.TOKEN_PREFIX
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtTokenFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val bearerToken = request.getHeader(HEADER_STRING)
        if (bearerToken == null || !bearerToken.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }
        val jwt = bearerToken.substring(7)
        val subjectUsername = jwtService.extractSubject(jwt)
        if (subjectUsername.isNotEmpty() && SecurityContextHolder.getContext().authentication == null) {
            val validToken = jwtService.validateToken(jwt)
            if (validToken) {
                val jwtUserDetails = jwtService.getJwtUserDetails(jwt)
                val authenticationToken =
                    UsernamePasswordAuthenticationToken(jwtUserDetails.userId, jwt, jwtUserDetails.authorities)
                authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authenticationToken
            }
        }
        filterChain.doFilter(request, response)
    }

}
