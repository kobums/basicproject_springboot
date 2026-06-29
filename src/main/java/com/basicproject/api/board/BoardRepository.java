package com.basicproject.api.board;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 목록/단건 조회 시 작성자(user)를 함께 fetch 해서 N+1 방지 (페이징과 호환되는 EntityGraph 사용)
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Board> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Board> findWithUserById(Long id);

    // ---- 검색 (작성자 user 를 함께 fetch). 대소문자 무시 부분일치(LIKE %kw%) ----
    @EntityGraph(attributePaths = "user")
    Page<Board> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Board> findByContentContainingIgnoreCase(String keyword, Pageable pageable);

    // 제목 OR 내용 (같은 키워드를 두 번 바인딩)
    @EntityGraph(attributePaths = "user")
    Page<Board> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String title, String content, Pageable pageable);

    // 작성자 이름 (user_tb.u_name)
    @EntityGraph(attributePaths = "user")
    Page<Board> findByUser_NameContainingIgnoreCase(String keyword, Pageable pageable);
}
