package com.frankie.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping


@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class NoHandlerFoundFilter(private val requestMappingHandler: RequestMappingHandlerMapping) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (requestMappingHandler.getHandler(request) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not foundddd")
            return
        }

        filterChain.doFilter(request, response)
    }

}