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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
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

    // 통일 JWT 형식의 토큰을 직접 만든다 (관리자 level 등). SecuritySessionProvider.currentValue("level") 이 이 값을 읽는다.
    private fun tokenFor(id: Long, level: Int): String {
        val now = System.currentTimeMillis() / 1000
        return Jwts.builder()
            .claims(mapOf("sub" to id.toString(), "id" to id, "email" to "t@example.com", "level" to level, "iat" to now, "exp" to now + 3600))
            .signWith(key(), Jwts.SIG.HS256)
            .compact()
    }

    @Test
    fun signupLoginMeAndTokenFormat() {
        val email = uniqueEmail()
        val signupBody = mvc.perform(json(post("/api/auth/signup"), """{"email":"$email","name":"홍길동","password":"pw1234"}"""))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresInMs").value(86_400_000))
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.level").value(1))
            .andExpect(jsonPath("$.user.name").value("홍길동"))
            .andExpect(jsonPath("$.user.createddate").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
            .andExpect(jsonPath("$.user.passwd").doesNotExist())
            .body()
        val id = JsonPath.read<Number>(signupBody, "$.user.id").toLong()

        // 같은 이메일은 409 {status, message} (fieldErrors 없음)
        mvc.perform(json(post("/api/auth/signup"), """{"email":"$email","name":"x","password":"pw1234"}"""))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.fieldErrors").doesNotExist())
        // 형식 오류는 400 + fieldErrors
        mvc.perform(json(post("/api/auth/signup"), """{"email":"not-an-email","password":"1"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.fieldErrors.email").exists())

        // 로그인 실패는 401 (없는 이메일도 같다)
        mvc.perform(json(post("/api/auth/login"), """{"email":"$email","password":"wrong"}"""))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
        mvc.perform(json(post("/api/auth/login"), """{"email":"nobody-$email","password":"pw1234"}"""))
            .andExpect(status().isUnauthorized)
        val loginBody = mvc.perform(json(post("/api/auth/login"), """{"email":"$email","password":"pw1234"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresInMs").value(86_400_000))
            .andExpect(jsonPath("$.user.id").value(id))
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
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.level").value(1))
            .andExpect(jsonPath("$.passwd").doesNotExist())
        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun generatedBoardUsesSessionOwner() {
        val (token, id) = signup()
        val (otherToken, otherId) = signup()

        // 본문의 user 는 무시하고 로그인 사용자로 정한다 (model.json board session)
        val created = mvc.perform(json(post("/api/board"), """{"title":"hello","content":"body","user":999999}""", token))
            .andExpect(status().is2xxSuccessful)
            .andExpect(jsonPath("$.title").value("hello"))
            .andExpect(jsonPath("$.user").value(id))
            // join user: 작성자 정보 (비밀번호 없음)
            .andExpect(jsonPath("$.extra.user.id").value(id))
            .andExpect(jsonPath("$.extra.user.passwd").doesNotExist())
            .body()
        val boardId = JsonPath.read<Number>(created, "$.id").toLong()

        mvc.perform(auth(get("/api/board"), token).param("user", id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].user").value(id))
            .andExpect(jsonPath("$.content[0].extra.user.email").exists())

        // read: "login" → 다른 로그인 사용자도 읽는다
        mvc.perform(auth(get("/api/board"), otherToken).param("user", id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
        mvc.perform(auth(get("/api/board/$boardId"), otherToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user").value(id))

        // 수정 / 삭제는 작성자만: 남의 글은 404, 일괄 삭제 403
        mvc.perform(json(put("/api/board/$boardId"), """{"title":"hacked"}""", otherToken)).andExpect(status().isNotFound)
        mvc.perform(json(patch("/api/board/$boardId"), """{"title":"hacked"}""", otherToken)).andExpect(status().isNotFound)
        mvc.perform(auth(delete("/api/board/$boardId"), otherToken)).andExpect(status().isNotFound)
        mvc.perform(json(delete("/api/board/batch"), """[{"id":$boardId}]""", otherToken)).andExpect(status().isForbidden)
        // 작성자를 바꾸는 수정도 세션 값으로 고정된다
        mvc.perform(json(patch("/api/board/$boardId"), """{"title":"edited","user":$otherId}""", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("edited"))
            .andExpect(jsonPath("$.user").value(id))
        mvc.perform(auth(delete("/api/board/$boardId"), token)).andExpect(status().isOk)

        // 토큰 없음 → 생성 컨트롤러가 401
        mvc.perform(json(post("/api/board"), """{"title":"x"}""")).andExpect(status().isUnauthorized)
    }

    @Test
    fun anonymousBoardReadIs401() {
        mvc.perform(get("/api/board")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/board/count")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/board/1")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/board/search/title").param("title", "x")).andExpect(status().isUnauthorized)
    }

    @Test
    fun boardStringFilterIsPartialMatchWithLiteralWildcards() {
        val (token, id) = signup()
        val tag = UUID.randomUUID().toString().take(8)
        for (title in listOf("${tag} 50% off", "${tag} 50 off", "${tag} a_b", "${tag} axb", "${tag} hello world")) {
            mvc.perform(json(post("/api/board"), """{"title":"$title"}""", token)).andExpect(status().is2xxSuccessful)
        }
        fun titles(q: String): List<String> {
            val body = mvc.perform(auth(get("/api/board"), token).param("user", id.toString()).param("title", q))
                .andExpect(status().isOk).body()
            return JsonPath.read<List<String>>(body, "$.content[*].title").sorted()
        }
        // 부분 일치 (like '%...%')
        assertEquals(listOf("$tag hello world"), titles("lo wor"))
        assertEquals(5, titles(tag).size)
        // % 와 _ 는 글자 그대로 (escape '!')
        assertEquals(listOf("$tag 50% off"), titles("50%"))
        assertEquals(listOf("$tag a_b"), titles("a_b"))
        assertEquals(emptyList<String>(), titles("!"))
        // 개수도 같은 필터
        mvc.perform(auth(get("/api/board/count"), token).param("user", id.toString()).param("title", "_"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
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
        // board 는 read: "login" 이라 다른 테스트의 글도 보인다: 이 사용자의 글만 걸러 확인한다
        mvc.perform(auth(get("/api/board"), minimal).param("user", id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].title").value("from go"))
    }

    @Test
    fun generatedUserInsertIsOpenAndSharesPasswordHash() {
        val email = uniqueEmail()
        // 생성된 POST /api/user (회원가입) 는 로그인 없이 된다 (Go 와 같다)
        // level 은 보호 컬럼: 보낸 값 대신 DB 기본값 1 (u_level DEFAULT 1)
        mvc.perform(json(post("/api/user"), """{"email":"$email","name":"gen","passwd":"pw1234","level":9}"""))
            .andExpect(status().is2xxSuccessful)
            .andExpect(jsonPath("$.level").value(1))
            .andExpect(content().string(not(containsString("pw1234"))))
        // 같은 PasswordEncoder 로 해시했으므로 /api/auth/login 이 된다
        mvc.perform(json(post("/api/auth/login"), """{"email":"$email","password":"pw1234"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.level").value(1))
        // 목록은 로그인 필요
        mvc.perform(get("/api/user")).andExpect(status().isUnauthorized)
        mvc.perform(json(post("/api/user/batch"), "[]")).andExpect(status().isUnauthorized)
    }

    @Test
    fun userReadsOnlySelfUnlessAdmin() {
        val (token, id) = signup()
        val (_, otherId) = signup()

        // 일반 회원: 목록 / 개수 / 검색은 본인 행만, 남의 단건은 404
        mvc.perform(auth(get("/api/user"), token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(id))
            .andExpect(jsonPath("$.content[0].passwd").doesNotExist())
        mvc.perform(auth(get("/api/user/count"), token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
        mvc.perform(auth(get("/api/user/search/name"), token).param("name", "tester"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
        mvc.perform(auth(get("/api/user/$id"), token)).andExpect(status().isOk)
        mvc.perform(auth(get("/api/user/$otherId"), token)).andExpect(status().isNotFound)
        // 남의 회원 정보 수정 / 삭제도 404 / 403
        mvc.perform(json(patch("/api/user/$otherId"), """{"name":"x"}""", token)).andExpect(status().isNotFound)
        mvc.perform(json(delete("/api/user/batch"), """[{"id":$otherId}]""", token)).andExpect(status().isForbidden)

        // level 2 도 일반 회원 (adminLevel 3 미만)
        mvc.perform(auth(get("/api/user/$otherId"), tokenFor(id, 2))).andExpect(status().isNotFound)

        // 관리자 (level >= adminLevel 3): 모든 행
        val admin = tokenFor(id, 3)
        mvc.perform(auth(get("/api/user/$otherId"), admin))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(otherId))
        val all = mvc.perform(auth(get("/api/user"), admin)).andExpect(status().isOk).body()
        assertTrue(JsonPath.read<Number>(all, "$.totalElements").toLong() >= 2)
        val ids = JsonPath.read<List<Number>>(all, "$.content[*].id").map { it.toLong() }
        assertTrue(id in ids && otherId in ids, ids.toString())
    }

    @Test
    fun uploadRequiresLoginAndImage() {
        val (token, _) = signup()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0)
        val body = mvc.perform(auth(multipart("/api/upload/index").file(MockMultipartFile("upload", "a.png", "image/png", png)), token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.filename").exists())
            .body()
        assertEquals(setOf("filename", "url"), JsonPath.read<Map<String, Any>>(body, "$").keys)
        val url = JsonPath.read<String>(body, "$.url")
        assertTrue(url.startsWith("/uploads/") && url.endsWith(".png"), url)
        mvc.perform(get(url)).andExpect(status().isOk)
        // 예전 경로 /api/upload 도 같다
        mvc.perform(auth(multipart("/api/upload").file(MockMultipartFile("upload", "b.png", "image/png", png)), token))
            .andExpect(status().isOk)
        // 파일 필드가 없으면 400 {status, message}
        mvc.perform(auth(multipart("/api/upload/index").file(MockMultipartFile("other", "a.png", "image/png", png)), token))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))

        // 확장자만 이미지인 파일은 거부
        mvc.perform(auth(multipart("/api/upload").file(MockMultipartFile("upload", "x.png", "image/png", "<svg/>".toByteArray())), token))
            .andExpect(status().isBadRequest)
        mvc.perform(multipart("/api/upload/index").file(MockMultipartFile("upload", "a.png", "image/png", png)))
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
