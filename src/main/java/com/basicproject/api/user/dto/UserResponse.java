package com.basicproject.api.user.dto;

import com.basicproject.api.user.User;
import java.time.LocalDateTime;

// 비밀번호(u_passwd)는 응답에 포함하지 않는다.
public record UserResponse(
        Long id,
        String email,
        String name,
        LocalDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getCreatedAt());
    }
}
