package com.basicproject.api.security

import com.basicproject.api.common.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

// 로그인이 필요한 경로(/api/auth/me, /api/upload)에 토큰이 없거나 잘못되면 JSON 401 을 돌려준다.
@Component
class RestAuthenticationEntryPoint(private val jsonMapper: JsonMapper) : AuthenticationEntryPoint {

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        jsonMapper.writeValue(response.writer, ErrorResponse.of(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."))
    }
}
