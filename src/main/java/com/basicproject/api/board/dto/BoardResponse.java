package com.basicproject.api.board.dto;

import com.basicproject.api.board.Board;
import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        String title,
        String content,
        String imgUrl,
        Author author,
        LocalDateTime createdAt) {

    // 작성자 요약 (user_tb 조인 결과)
    public record Author(Long id, String name) {
    }

    public static BoardResponse from(Board b) {
        return new BoardResponse(
                b.getId(),
                b.getTitle(),
                b.getContent(),
                toImgUrl(b.getImg()),
                new Author(b.getUser().getId(), b.getUser().getName()),
                b.getCreatedAt());
    }

    // 외부 URL 이면 그대로, 업로드 파일명이면 /uploads/ 경로로 변환
    private static String toImgUrl(String img) {
        if (img == null || img.isBlank()) {
            return null;
        }
        if (img.startsWith("http://") || img.startsWith("https://") || img.startsWith("/")) {
            return img;
        }
        return "/uploads/" + img;
    }
}
