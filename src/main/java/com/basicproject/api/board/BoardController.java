package com.basicproject.api.board;

import com.basicproject.api.board.dto.BoardCreateForm;
import com.basicproject.api.board.dto.BoardResponse;
import com.basicproject.api.board.dto.BoardUpdateForm;
import com.basicproject.api.common.PageResponse;
import com.basicproject.api.security.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService service;

    // type: title(기본) | content | all | author, keyword: 검색어(없으면 전체 목록)
    @GetMapping
    public PageResponse<BoardResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(type, keyword, pageable);
    }

    @GetMapping("/{id}")
    public BoardResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    // 이미지 업로드를 포함하므로 multipart/form-data 로 받는다.
    // 작성자는 폼 값이 아니라 JWT 인증 사용자(principal)에서 가져온다.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponse create(
            @Valid @ModelAttribute BoardCreateForm form,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return service.create(form, principal.id());
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BoardResponse update(
            @PathVariable Long id,
            @Valid @ModelAttribute BoardUpdateForm form,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return service.update(id, form, principal.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        service.delete(id, principal.id());
    }
}
