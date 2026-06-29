package com.basicproject.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 255) String name,
        @NotBlank @Size(min = 4, max = 255) String password) {
}
