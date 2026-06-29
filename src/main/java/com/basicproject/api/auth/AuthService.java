package com.basicproject.api.auth;

import com.basicproject.api.auth.dto.LoginRequest;
import com.basicproject.api.auth.dto.SignupRequest;
import com.basicproject.api.auth.dto.TokenResponse;
import com.basicproject.api.security.JwtProvider;
import com.basicproject.api.user.User;
import com.basicproject.api.user.UserRepository;
import com.basicproject.api.user.UserService;
import com.basicproject.api.user.dto.UserCreateRequest;
import com.basicproject.api.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 회원가입: 기존 UserService.create(중복검사 + 해싱)를 재사용하고 곧바로 토큰을 발급한다.
    @Transactional
    public TokenResponse signup(SignupRequest req) {
        UserResponse created = userService.create(
                new UserCreateRequest(req.email(), req.name(), req.password()));
        String token = jwtProvider.createToken(created.id(), created.email());
        return TokenResponse.of(token, jwtProvider.getExpirationMs(), created);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!matches(user, req.password())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtProvider.createToken(user.getId(), user.getEmail());
        return TokenResponse.of(token, jwtProvider.getExpirationMs(), UserResponse.from(user));
    }

    // 저장된 비밀번호가 BCrypt 해시면 그대로 검증.
    // 학습용으로 평문 저장된 기존 데이터는 평문 비교 후, 일치하면 BCrypt 로 자동 마이그레이션한다.
    private boolean matches(User user, String rawPassword) {
        String stored = user.getPassword();
        if (stored != null && stored.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        if (stored != null && stored.equals(rawPassword)) {
            user.changePassword(passwordEncoder.encode(rawPassword));
            return true;
        }
        return false;
    }
}
