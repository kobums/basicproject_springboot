package com.basicproject.api.auth;

import com.basicproject.api.auth.dto.LoginRequest;
import com.basicproject.api.auth.dto.SignupRequest;
import com.basicproject.api.auth.dto.TokenResponse;
import com.basicproject.api.security.AuthPrincipal;
import com.basicproject.api.user.UserService;
import com.basicproject.api.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest req) {
        return authService.signup(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    // 현재 토큰의 사용자 정보. 인증 필요(SecurityConfig 에서 anyRequest authenticated).
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return userService.get(principal.id());
    }
}
