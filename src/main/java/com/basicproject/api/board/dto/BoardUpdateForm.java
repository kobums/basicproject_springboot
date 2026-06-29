package com.basicproject.api.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

// 수정용. image 는 선택 — 새 파일이 오면 교체, 없으면 기존 이미지 유지.
@Getter
@Setter
public class BoardUpdateForm {

    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 255)
    private String content;

    private MultipartFile image;
}
