package com.basicproject.api.user;

import com.basicproject.api.user.dto.UserCreateRequest;
import com.basicproject.api.user.dto.UserResponse;
import com.basicproject.api.user.dto.UserUpdateRequest;
import com.basicproject.api.common.PageResponse;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserResponse> list(String keyword, Pageable pageable) {
        // 키워드가 없으면 전체, 있으면 이름/이메일 부분일치 검색.
        Page<User> page;
        if (keyword == null || keyword.isBlank()) {
            page = repository.findAll(pageable);
        } else {
            String kw = keyword.trim();
            page = repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(kw, kw, pageable);
        }
        return PageResponse.of(page.map(UserResponse::from));
    }

    public UserResponse get(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + req.email());
        }
        User user = User.builder()
                .email(req.email())
                .name(req.name())
                .password(passwordEncoder.encode(req.password()))
                .build();
        return UserResponse.from(repository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req, Long requesterId) {
        assertSelf(id, requesterId);
        User user = findOrThrow(id);
        // 비밀번호를 새로 입력한 경우에만 해시해서 교체 (빈 값이면 User.update 에서 무시).
        String encoded = (req.password() != null && !req.password().isBlank())
                ? passwordEncoder.encode(req.password())
                : null;
        user.update(req.name(), encoded);
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        assertSelf(id, requesterId);
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("user not found: " + id);
        }
        repository.deleteById(id);
    }

    private User findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + id));
    }

    // 본인 계정만 수정/삭제할 수 있다.
    private void assertSelf(Long targetId, Long requesterId) {
        if (!targetId.equals(requesterId)) {
            throw new AccessDeniedException("본인 계정만 수정/삭제할 수 있습니다.");
        }
    }
}
