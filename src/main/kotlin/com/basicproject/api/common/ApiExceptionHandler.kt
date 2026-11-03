package com.basicproject.api.common

import com.basicproject.api.auth.AuthController
import com.basicproject.api.upload.UploadController
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException

// 손으로 만든 컨트롤러에만 적용한다 (생성된 컨트롤러의 응답 형식은 gomachine 이 정한다).
@RestControllerAdvice(basePackageClasses = [AuthController::class, UploadController::class])
class ApiExceptionHandler {

    // @Valid @RequestBody (MethodArgumentNotValidException 은 BindException 의 하위 클래스)
    @ExceptionHandler(BindException::class)
    fun handleValidation(e: BindException): ResponseEntity<ErrorResponse> {
        val fields = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return build(HttpStatus.BAD_REQUEST, "validation failed", fields)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(e: HttpMessageNotReadableException) = build(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException) = build(HttpStatus.BAD_REQUEST, e.message ?: "bad request")

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException) = build(HttpStatus.NOT_FOUND, e.message ?: "not found")

    // 동시에 같은 이메일로 가입하는 경우 등 DB 제약 조건 위반
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConstraint(e: DataIntegrityViolationException) = build(HttpStatus.CONFLICT, "constraint violation")

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUpload(e: MaxUploadSizeExceededException) = build(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 용량이 너무 큽니다.")

    @ExceptionHandler(MultipartException::class)
    fun handleMultipart(e: MultipartException) = build(HttpStatus.BAD_REQUEST, "multipart/form-data 요청이 필요합니다.")

    private fun build(status: HttpStatus, message: String, fields: Map<String, String> = emptyMap()) =
        ResponseEntity.status(status).body(ErrorResponse.of(status, message, fields))
}
