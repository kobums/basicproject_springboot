package com.basicproject.api.board;

import com.basicproject.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 기존 board_tb 테이블에 매핑. b_user 는 user_tb 로의 FK (@ManyToOne).
@Entity
@Table(name = "board_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "b_id")
    private Long id;

    @Column(name = "b_title")
    private String title;

    @Column(name = "b_content", nullable = false)
    private String content;

    // 업로드된 이미지의 파일명(또는 외부 URL). 실제 응답에서는 imgUrl 로 변환.
    @Column(name = "b_img", nullable = false)
    private String img;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "b_user", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "b_date", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Board(String title, String content, String img, User user) {
        this.title = title;
        this.content = content;
        this.img = img;
        this.user = user;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void changeImg(String img) {
        this.img = img;
    }
}
