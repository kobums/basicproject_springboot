package com.basicproject.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 기존 user_tb 테이블에 매핑 (스키마 수동 관리, JPA 는 변경하지 않음)
@Entity
@Table(name = "user_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    private Long id;

    @Column(name = "u_email", nullable = false)
    private String email;

    @Column(name = "u_name")
    private String name;

    @Column(name = "u_passwd", nullable = false)
    private String password;

    @CreatedDate
    @Column(name = "u_date", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }

    public void update(String name, String password) {
        this.name = name;
        if (password != null && !password.isBlank()) {
            this.password = password;
        }
    }

    // 비밀번호 해시 교체용 (로그인 시 평문 → BCrypt 자동 마이그레이션 등).
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
