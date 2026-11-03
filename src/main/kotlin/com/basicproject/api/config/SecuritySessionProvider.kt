package com.basicproject.api.config

import com.basicproject.api.security.AuthPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

// gomachine 이 만든 컨트롤러가 쓰는 SessionProvider 구현 (사용자 파일, 생성기가 건드리지 않는다).
// JwtAuthenticationFilter 가 SecurityContext 에 넣은 AuthPrincipal 을 읽는다.
@Component
class SecuritySessionProvider : SessionProvider {

    private fun principal(): AuthPrincipal? =
        SecurityContextHolder.getContext().authentication?.principal as? AuthPrincipal

    override fun currentUserId(): Long? = principal()?.id

    // model.json table[].session 의 key: "id" (로그인 사용자 id), "level"
    override fun currentValue(key: String): Long? = when (key) {
        "id" -> currentUserId()
        "level" -> principal()?.level?.toLong()
        else -> null
    }
}
