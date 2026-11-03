package com.basicproject.api.common

import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 손으로 만든 API(/api/auth, /api/upload)의 에러 본문. 생성된 API 는 상태 코드(400/401/403/404/409)로 알린다.
data class ErrorResponse(
    val status: Int,
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
) {
    companion object {
        fun of(status: HttpStatus, message: String, fieldErrors: Map<String, String> = emptyMap()) =
            ErrorResponse(status.value(), message, fieldErrors)
    }
}
