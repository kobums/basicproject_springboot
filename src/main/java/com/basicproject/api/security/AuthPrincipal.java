package com.basicproject.api.security;

// SecurityContext 의 Authentication principal 로 쓰이는 인증된 사용자 정보.
// 컨트롤러에서 @AuthenticationPrincipal AuthPrincipal 로 주입받을 수 있다.
public record AuthPrincipal(Long id, String email) {
}
