package com.basicproject.api.auth

import com.basicproject.api.entity.User
import com.basicproject.api.entity.UserResponse
import com.basicproject.api.repository.UserRepository
import com.basicproject.api.security.JwtProvider
import com.basicproject.api.support.DateParams
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 회원가입 / 로그인. 생성된 User 엔티티와 UserRepository 를 그대로 쓴다 (이메일 조회는 Specification).
@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {
    companion object {
        // 회원가입 기본 등급 (db/schema.sql 의 u_level 기본값). 관리자 등급은 model.json adminLevel.
        const val DEFAULT_LEVEL = 1
        private const val LOGIN_FAILED = "이메일 또는 비밀번호가 올바르지 않습니다."
    }

    private fun byEmail(email: String) =
        Specification<User> { root, _, cb -> cb.equal(root.get<String>("email"), email) }

    @Transactional
    fun signup(req: SignupRequest): TokenResponse {
        val email = req.email.trim()
        require(userRepository.count(byEmail(email)) == 0L) { "이미 사용 중인 이메일입니다: $email" }
        val user = userRepository.save(
            User(
                email = email,
                passwd = passwordEncoder.encode(req.password)!!,
                name = req.name.trim(),
                level = DEFAULT_LEVEL,
                createddate = DateParams.now(),
            )
        )
        return token(user)
    }

    fun login(req: LoginRequest): TokenResponse {
        val user = userRepository.findAll(byEmail(req.email.trim())).firstOrNull()
            ?: throw IllegalArgumentException(LOGIN_FAILED)
        if (!user.passwd.startsWith("$2") || !passwordEncoder.matches(req.password, user.passwd)) {
            throw IllegalArgumentException(LOGIN_FAILED)
        }
        return token(user)
    }

    fun me(id: Long): UserResponse =
        userRepository.findById(id).map { UserResponse.from(it) }.orElseThrow { NoSuchElementException("user not found") }

    private fun token(user: User) = TokenResponse(
        token = jwtProvider.createToken(user.id, user.email, user.level),
        expiresInMs = jwtProvider.expirationMs,
        user = UserResponse.from(user),
    )
}
