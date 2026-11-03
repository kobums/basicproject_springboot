package com.basicproject.api.auth

import com.basicproject.api.entity.UserResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:NotBlank @field:Email @field:Size(max = 255)
    val email: String = "",
    @field:NotBlank @field:Size(min = 4, max = 255)
    val password: String = "",
    @field:Size(max = 100)
    val name: String = "",
)

data class LoginRequest(
    @field:NotBlank
    val email: String = "",
    @field:NotBlank
    val password: String = "",
)

// 로그인 / 회원가입 응답 (Go 템플릿과 같은 모양). user 는 생성된 UserResponse (비밀번호 없음).
data class TokenResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresInMs: Long,
    val user: UserResponse,
)
