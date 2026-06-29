package com.basicproject.api.board;

import com.basicproject.api.board.dto.BoardCreateForm;
import com.basicproject.api.board.dto.BoardResponse;
import com.basicproject.api.board.dto.BoardUpdateForm;
import com.basicproject.api.common.PageResponse;
import com.basicproject.api.file.FileStorageService;
import com.basicproject.api.user.User;
import com.basicproject.api.user.UserRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorage;

    public PageResponse<BoardResponse> list(String type, String keyword, Pageable pageable) {
        // 키워드가 없으면 전체 목록, 있으면 type 에 따라 검색.
        Page<Board> page;
        if (keyword == null || keyword.isBlank()) {
            page = boardRepository.findAll(pageable);
        } else {
            String kw = keyword.trim();
            page = switch (type == null ? "title" : type) {
                case "content" -> boardRepository.findByContentContainingIgnoreCase(kw, pageable);
                case "all" ->
                    boardRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(kw, kw, pageable);
                case "author" -> boardRepository.findByUser_NameContainingIgnoreCase(kw, pageable);
                default -> boardRepository.findByTitleContainingIgnoreCase(kw, pageable);
            };
        }
        return PageResponse.of(page.map(BoardResponse::from));
    }

    public BoardResponse get(Long id) {
        Board board = boardRepository.findWithUserById(id)
                .orElseThrow(() -> new NoSuchElementException("board not found: " + id));
        return BoardResponse.from(board);
    }

    @Transactional
    public BoardResponse create(BoardCreateForm form, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + userId));

        // 이미지는 선택값: 없으면 빈 문자열로 저장(b_img NOT NULL 대응, 응답에선 imgUrl=null).
        MultipartFile image = form.getImage();
        String filename = (image != null && !image.isEmpty()) ? fileStorage.store(image) : "";

        Board board = Board.builder()
                .title(form.getTitle())
                .content(form.getContent())
                .img(filename)
                .user(user)
                .build();
        return BoardResponse.from(boardRepository.save(board));
    }

    @Transactional
    public BoardResponse update(Long id, BoardUpdateForm form, Long requesterId) {
        Board board = boardRepository.findWithUserById(id)
                .orElseThrow(() -> new NoSuchElementException("board not found: " + id));
        assertOwner(board, requesterId);

        board.update(form.getTitle(), form.getContent());

        MultipartFile image = form.getImage();
        if (image != null && !image.isEmpty()) {
            String old = board.getImg();
            board.changeImg(fileStorage.store(image));
            fileStorage.deleteQuietly(old);
        }
        return BoardResponse.from(board);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        Board board = boardRepository.findWithUserById(id)
                .orElseThrow(() -> new NoSuchElementException("board not found: " + id));
        assertOwner(board, requesterId);
        String img = board.getImg();
        boardRepository.delete(board);
        fileStorage.deleteQuietly(img);
    }

    // 게시글 작성자 본인만 수정/삭제할 수 있다.
    private void assertOwner(Board board, Long requesterId) {
        if (!board.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("본인이 작성한 게시글만 수정/삭제할 수 있습니다.");
        }
    }
}
