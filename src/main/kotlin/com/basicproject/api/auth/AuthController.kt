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

//   POST /api/auth/signup  회원가입 (level 1) 후 토큰 발급
//   POST /api/auth/login   로그인 후 토큰 발급
//   GET  /api/auth/me      현재 토큰의 사용자 (로그인 필요)
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
