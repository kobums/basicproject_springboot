package com.basicproject.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// app.jwt.secret 은 기본값이 없어(미설정 시 기동 실패) 테스트 전용 값을 준다
@SpringBootTest(properties = "app.jwt.secret=test-only-jwt-secret-not-for-production-0123456789")
class BasicprojectApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
