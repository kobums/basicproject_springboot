package com.basicproject.api.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

// multipart/form-data 바인딩용 (제목/내용 + 선택적 이미지 파일).
// 작성자는 폼이 아니라 JWT(인증 사용자)에서 가져온다. 이미지는 선택값이다.
@Getter
@Setter
public class BoardCreateForm {

    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 255)
    private String content;

    private MultipartFile image;
}
