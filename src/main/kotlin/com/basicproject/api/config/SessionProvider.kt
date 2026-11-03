package com.basicproject.api.config

// 생성된 컨트롤러가 로그인 확인과 소유권 검사에 쓰는 현재 로그인 사용자 정보 (auth=jwt).
// 대상 프로젝트에서 인증 방식(JWT 필터, Spring Security 등)에 맞게 @Component 로 구현해야 한다.
interface SessionProvider {
    // 현재 로그인 사용자의 id. 비로그인이면 null (생성된 엔드포인트가 401 을 돌려준다).
    fun currentUserId(): Long?

    // model.json session 의 key 에 해당하는 세션 값. 기본은 "id" 만 지원한다.
    fun currentValue(key: String): Long? = if (key == "id") currentUserId() else null
}
