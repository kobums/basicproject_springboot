package com.basicproject.api.common

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus

// 손으로 만든 API(/api/auth, /api/upload)의 에러 본문: {status, message, fieldErrors?} (gomachine README "인증 API 계약").
// fieldErrors 는 검증 실패일 때만 있다 ({필드: 메시지}). 생성된 API 는 상태 코드(400/401/403/404/409)로 알린다.
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val status: Int,
    val message: String,
    val fieldErrors: Map<String, String>? = null,
) {
    companion object {
        fun of(status: HttpStatus, message: String, fieldErrors: Map<String, String>? = null) =
            ErrorResponse(status.value(), message, fieldErrors?.ifEmpty { null })
    }
}

// 상태 코드를 정해서 던지는 예외 (ApiExceptionHandler 가 ErrorResponse 로 바꾼다).
class ApiException(val status: HttpStatus, message: String) : RuntimeException(message)
