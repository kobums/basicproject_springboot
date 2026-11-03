package com.basicproject.api

import com.jayway.jsonpath.JsonPath
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Date
import java.util.UUID

// 인증(손으로 만든 코드) + gomachine 생성 API 를 H2 위에서 끝까지 돌려 본다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Value("\${app.jwt.secret}")
    lateinit var secret: String

    private fun json(builder: MockHttpServletRequestBuilder, body: String, token: String? = null): MockHttpServletRequestBuilder {
        builder.contentType(MediaType.APPLICATION_JSON).content(body)
        if (token != null) builder.header("Authorization", "Bearer $token")
        return builder
    }

    private fun <B : AbstractMockHttpServletRequestBuilder<B>> auth(builder: B, token: String): B =
        builder.header("Authorization", "Bearer $token")

    private fun ResultActions.body(): String = andReturn().response.getContentAsString(StandardCharsets.UTF_8)

    private fun uniqueEmail() = "u${UUID.randomUUID().toString().take(8)}@example.com"

    // 회원가입 후 (token, id)
    private fun signup(email: String = uniqueEmail(), password: String = "pw1234"): Pair<String, Long> {
        val body = mvc.perform(json(post("/api/auth/signup"), """{"email":"$email","name":"tester","password":"$password"}"""))
            .andExpect(status().isCreated)
            .body()
        return JsonPath.read<String>(body, "$.token") to JsonPath.read<Number>(body, "$.user.id").toLong()
    }

    private fun key() = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun signupLoginMeAndTokenFormat() {
        val email = uniqueEmail()
        val signupBody = mvc.perform(json(post("/api/auth/signup"), """{"email":"$email","name":"홍길동","password":"pw1234"}"""))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresInMs").value(86_400_000))
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.level").value(1))
            .andExpect(jsonPath("$.user.passwd").doesNotExist())
            .body()
        val id = JsonPath.read<Number>(signupBody, "$.user.id").toLong()

        // 같은 이메일은 400
        mvc.perform(json(post("/api/auth/signup"), """{"email":"$email","name":"x","password":"pw1234"}"""))
            .andExpect(status().isBadRequest)
        // 형식 오류는 400 + fieldErrors
        mvc.perform(json(post("/api/auth/signup"), """{"email":"not-an-email","password":"1"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.email").exists())

        mvc.perform(json(post("/api/auth/login"), """{"email":"$email","password":"wrong"}"""))
            .andExpect(status().isBadRequest)
        val loginBody = mvc.perform(json(post("/api/auth/login"), """{"email":"$email","password":"pw1234"}"""))
            .andExpect(status().isOk)
            .body()
        val token = JsonPath.read<String>(loginBody, "$.token")

        // 통일 JWT: HS256, {sub, id, email, level, iat, exp} (초 단위 정수, 24시간)
        val parts = token.split('.')
        val header = String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
        assertEquals("HS256", JsonPath.read<String>(header, "$.alg"))
        assertEquals(id.toString(), JsonPath.read<String>(payload, "$.sub"))
        assertEquals(id, JsonPath.read<Number>(payload, "$.id").toLong())
        assertEquals(email, JsonPath.read<String>(payload, "$.email"))
        assertEquals(1, JsonPath.read<Number>(payload, "$.level").toInt())
        val iat = JsonPath.read<Any>(payload, "$.iat")
        val exp = JsonPath.read<Any>(payload, "$.exp")
        assertTrue(iat is Int || iat is Long, "iat must be an integer: $iat")
        assertEquals(86_400L, (exp as Number).toLong() - (iat as Number).toLong())

        mvc.perform(auth(get("/api/auth/me"), token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.email").value(email))
        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun generatedBoardUsesSessionOwner() {
        val (token, id) = signup()
        val (otherToken, _) = signup()

        // 본문의 user 는 무시하고 로그인 사용자로 정한다 (model.json board session)
        mvc.perform(json(post("/api/board"), """{"title":"hello","content":"body","user":999999}""", token))
            .andExpect(status().is2xxSuccessful)
            .andExpect(jsonPath("$.title").value("hello"))
            .andExpect(jsonPath("$.user").value(id))

        mvc.perform(auth(get("/api/board"), token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].user").value(id))
        // 다른 사용자에게는 보이지 않는다
        mvc.perform(auth(get("/api/board"), otherToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))

        // 토큰 없음 → 생성 컨트롤러가 401
        mvc.perform(get("/api/board")).andExpect(status().isUnauthorized)
        mvc.perform(json(post("/api/board"), """{"title":"x"}""")).andExpect(status().isUnauthorized)
    }

    @Test
    fun badTokensAre401Not500() {
        val now = System.currentTimeMillis() / 1000
        val otherKey = Keys.hmacShaKeyFor("another-secret-another-secret-another-secret-0123".toByteArray())
        val tokens = listOf(
            "garbage",
            "a.b.c",
            "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIiwiaWQiOjF9.",
            // 다른 키
            Jwts.builder().subject("1").claim("id", 1).signWith(otherKey, Jwts.SIG.HS256).compact(),
            // 만료
            Jwts.builder().subject("1").claim("id", 1).issuedAt(Date((now - 100) * 1000)).expiration(Date((now - 10) * 1000))
                .signWith(key(), Jwts.SIG.HS256).compact(),
            // HS256 이 아닌 알고리즘 (같은 키)
            Jwts.builder().subject("1").claim("id", 1).signWith(key(), Jwts.SIG.HS512).compact(),
            // id / sub 가 숫자가 아님
            Jwts.builder().subject("abc").signWith(key(), Jwts.SIG.HS256).compact(),
        )
        for (t in tokens) {
            mvc.perform(auth(get("/api/board"), t)).andExpect(status().isUnauthorized)
            mvc.perform(auth(get("/api/auth/me"), t)).andExpect(status().isUnauthorized)
        }
        // 잘못된 토큰이 붙어 있어도 로그인 / 회원가입은 된다
        mvc.perform(json(post("/api/auth/signup"), """{"email":"${uniqueEmail()}","password":"pw1234"}""", "garbage"))
            .andExpect(status().isCreated)
    }

    @Test
    fun acceptsTokenIssuedByOtherBackends() {
        val (_, id) = signup()
        val now = System.currentTimeMillis() / 1000
        // Go / Rust 템플릿이 만드는 것과 같은 claim (JJWT 빌더가 아닌 다른 라이브러리가 만든 것처럼 claim 만 맞춘다)
        val goToken = Jwts.builder()
            .claims(mapOf("sub" to id.toString(), "id" to id, "email" to "go@example.com", "level" to 1, "iat" to now, "exp" to now + 86_400))
            .signWith(key(), Jwts.SIG.HS256)
            .compact()
        mvc.perform(json(post("/api/board"), """{"title":"from go"}""", goToken))
            .andExpect(status().is2xxSuccessful)
            .andExpect(jsonPath("$.user").value(id))
        mvc.perform(auth(get("/api/auth/me"), goToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        // id 가 없으면 sub, level 이 없어도 된다
        val minimal = Jwts.builder().subject(id.toString()).expiration(Date((now + 60) * 1000))
            .signWith(key(), Jwts.SIG.HS256).compact()
        mvc.perform(auth(get("/api/board"), minimal))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun generatedUserInsertIsOpenAndSharesPasswordHash() {
        val email = uniqueEmail()
        // 생성된 POST /api/user (회원가입) 는 로그인 없이 된다 (Go 와 같다)
        mvc.perform(json(post("/api/user"), """{"email":"$email","name":"gen","passwd":"pw1234"}"""))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().string(not(containsString("pw1234"))))
        // 같은 PasswordEncoder 로 해시했으므로 /api/auth/login 이 된다
        mvc.perform(json(post("/api/auth/login"), """{"email":"$email","password":"pw1234"}"""))
            .andExpect(status().isOk)
        // 목록은 로그인 필요
        mvc.perform(get("/api/user")).andExpect(status().isUnauthorized)
        mvc.perform(json(post("/api/user/batch"), "[]")).andExpect(status().isUnauthorized)
    }

    @Test
    fun uploadRequiresLoginAndImage() {
        val (token, _) = signup()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0)
        val body = mvc.perform(auth(multipart("/api/upload").file(MockMultipartFile("upload", "a.png", "image/png", png)), token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.original").value("a.png"))
            .body()
        val url = JsonPath.read<String>(body, "$.url")
        assertTrue(url.startsWith("/uploads/") && url.endsWith(".png"), url)
        mvc.perform(get(url)).andExpect(status().isOk)

        // 확장자만 이미지인 파일은 거부
        mvc.perform(auth(multipart("/api/upload").file(MockMultipartFile("upload", "x.png", "image/png", "<svg/>".toByteArray())), token))
            .andExpect(status().isBadRequest)
        mvc.perform(multipart("/api/upload").file(MockMultipartFile("upload", "a.png", "image/png", png)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun corsPreflightAllowsAuthorizationAndPatch() {
        mvc.perform(
            options("/api/board/1")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "authorization,content-type")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")))
    }
}
