package com.basicproject.api.user.dto;

import jakarta.validation.constraints.Size;

// 비밀번호는 비워두면 기존 값 유지 (User.update 에서 처리)
public record UserUpdateRequest(
        @Size(max = 255) String name,
        @Size(max = 255) String password) {
}
