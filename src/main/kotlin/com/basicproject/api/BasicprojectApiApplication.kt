package com.basicproject.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

// 로그인은 JWT 만 쓴다 (폼 로그인 / 기본 사용자 없음).
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class BasicprojectApiApplication

fun main(args: Array<String>) {
    runApplication<BasicprojectApiApplication>(*args)
}
