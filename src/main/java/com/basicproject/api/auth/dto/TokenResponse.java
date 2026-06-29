package com.basicproject.api.auth.dto;

import com.basicproject.api.user.dto.UserResponse;

// 로그인/회원가입 성공 시 토큰과 사용자 정보를 함께 반환한다.
public record TokenResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user) {

    public static TokenResponse of(String token, long expiresInMs, UserResponse user) {
        return new TokenResponse(token, "Bearer", expiresInMs, user);
    }
}
