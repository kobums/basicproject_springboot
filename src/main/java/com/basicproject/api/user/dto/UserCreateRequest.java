package com.basicproject.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String password) {
}
