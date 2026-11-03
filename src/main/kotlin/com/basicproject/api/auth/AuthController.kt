package com.basicproject.api.auth

import com.basicproject.api.entity.UserResponse
import com.basicproject.api.security.AuthPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

// gomachine README "인증 API 계약" (Go / Spring / Rust 템플릿 공통):
//   POST /api/auth/signup  {email, password, name} → 201 {token, tokenType, expiresInMs, user} | 400 검증 | 409 이미 있는 email
//   POST /api/auth/login   {email, password}       → 200 {token, tokenType, expiresInMs, user} | 401
//   GET  /api/auth/me      Authorization: Bearer   → 200 user | 401
//   user = {id, email, name, level, createddate}, 에러 = {status, message, fieldErrors?}
@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody req: SignupRequest): TokenResponse = authService.signup(req)

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): TokenResponse = authService.login(req)

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: AuthPrincipal): UserResponse = authService.me(principal.id)
}
