package com.basicproject.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

// Authorization: Bearer <token> 이 유효하면 SecurityContext 에 AuthPrincipal 을 넣는다.
// 토큰이 없거나 잘못되면 비로그인으로 계속 진행한다 (Go 의 nil Session, Rust 의 None 과 같다).
// 보호된 경로는 RestAuthenticationEntryPoint 가, 생성된 API 는 컨트롤러가 401 을 돌려준다.
// SecurityConfig 에서 직접 만든다 (@Component 로 두면 서블릿 필터로 한 번 더 등록된다).
class JwtAuthenticationFilter(private val jwtProvider: JwtProvider) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val principal = bearer(request)?.let { jwtProvider.parse(it) }
        if (principal != null) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities)
        }
        filterChain.doFilter(request, response)
    }

    private fun bearer(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")?.trim() ?: return null
        val space = header.indexOf(' ')
        if (space < 0 || !header.substring(0, space).equals("Bearer", ignoreCase = true)) return null
        return header.substring(space + 1).trim().ifEmpty { null }
    }
}
