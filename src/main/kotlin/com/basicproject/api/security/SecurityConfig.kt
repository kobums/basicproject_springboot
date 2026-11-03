package com.basicproject.api.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val authenticationEntryPoint: RestAuthenticationEntryPoint,
    @Value("\${app.cors.allowed-origins:http://localhost:*}") private val corsOrigins: String,
) {

    // 회원가입(AuthService)과 생성된 UserService 가 같이 쓰는 비밀번호 해시 (bcrypt, Go / Rust 와 같은 형식).
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
                    .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                    // 손으로 만든 API 중 로그인이 필요한 것
                    .requestMatchers("/api/auth/**", "/api/upload", "/api/upload/**").authenticated()
                    // 그 밖의 /api/** 는 gomachine 이 만든 컨트롤러다. 컨트롤러가 SessionProvider 로 직접
                    // 로그인 확인(401)과 소유권 검사를 한다 (POST /api/user 회원가입만 로그인 없이 된다, Go 와 같다).
                    .requestMatchers("/api/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(authenticationEntryPoint) }
            .addFilterBefore(JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = corsOrigins.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
            exposedHeaders = listOf("X-Truncated")
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
            registerCorsConfiguration("/uploads/**", config)
        }
    }
}
