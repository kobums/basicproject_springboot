package com.basicproject.api.security

// JWT 에서 꺼낸 로그인 사용자 (SecurityContext 의 principal).
// 컨트롤러에서 @AuthenticationPrincipal principal: AuthPrincipal 로 받을 수 있다.
data class AuthPrincipal(
    val id: Long,
    val email: String,
    val level: Int,
)
