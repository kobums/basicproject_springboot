package com.basicproject.api.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

// 통일 JWT (Go / Spring / Rust 백엔드 공통 형식).
//   헤더 Authorization: Bearer <token>, 알고리즘 HS256 고정, 키 = 환경변수 JWT_SECRET 의 UTF-8 바이트 (32 바이트 이상)
//   claim {"sub": "42", "id": 42, "email": "a@b.c", "level": 1, "iat": 초, "exp": 초}
//   읽을 때: id 가 없으면 sub 를 숫자로, level 은 없으면 0, exp 는 있으면 검증한다.
@Component
class JwtProvider(
    @Value("\${app.jwt.secret:}") secret: String,
    @Value("\${app.jwt.expiration-ms:86400000}") val expirationMs: Long,
) {
    private val ttlSeconds = expirationMs / 1000

    private val key: SecretKey

    init {
        // 기본 키를 두지 않는다. 없거나 짧으면 기동할 때 바로 실패한다.
        val bytes = secret.toByteArray(StandardCharsets.UTF_8)
        check(bytes.size >= 32) {
            "JWT_SECRET (app.jwt.secret) 이 없거나 32 바이트보다 짧습니다. .env 또는 환경변수로 설정하세요."
        }
        key = Keys.hmacShaKeyFor(bytes)
    }

    fun createToken(id: Long, email: String, level: Int): String {
        val now = System.currentTimeMillis() / 1000
        return Jwts.builder()
            .subject(id.toString())
            .claim("id", id)
            .claim("email", email)
            .claim("level", level)
            .issuedAt(Date(now * 1000))
            .expiration(Date((now + ttlSeconds) * 1000))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    // 검증에 성공하면 principal, 서명 / 만료 / 형식 / 알고리즘이 틀리면 null (예외를 밖으로 던지지 않는다).
    fun parse(token: String): AuthPrincipal? {
        return try {
            val jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            if (jws.header.algorithm != "HS256") return null
            val claims = jws.payload
            val id = (claims["id"] as? Number)?.toLong()
                ?: claims.subject?.trim()?.toLongOrNull()
                ?: return null
            if (id <= 0) return null
            val email = claims["email"] as? String ?: ""
            val level = (claims["level"] as? Number)?.toInt() ?: 0
            AuthPrincipal(id, email, level)
        } catch (e: Exception) {
            null
        }
    }
}
