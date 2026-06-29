# basicproject_api

Spring Boot 기반 게시판 REST API. JWT 인증, 회원/게시글 CRUD, 이미지 업로드를 제공한다.

## 기술 스택

- Java 21, Spring Boot 3.5.3
- Spring Web / Spring Data JPA / Spring Security
- JWT (JJWT 0.12.x, HS256)
- MariaDB
- Gradle

## 프로젝트 구조

```
src/main/java/com/basicproject/api
├── auth        # 회원가입/로그인/내 정보 (JWT 발급)
├── board       # 게시글 CRUD + 이미지 업로드
├── user        # 회원 CRUD
├── file        # 업로드 파일 저장 (확장자/타입 화이트리스트)
├── security    # JWT 발급·검증, Spring Security 설정
├── common      # 전역 예외 처리, 페이지네이션 응답
└── config      # 정적 리소스(/uploads) 서빙 설정
```

## 실행 방법

### 1. 환경 설정

비밀정보(DB 접속·JWT 키)는 git 에 커밋하지 않는다. 두 가지 방법 중 하나로 주입한다.

**(A) 로컬 설정 파일** — `src/main/resources/application-local.yml` 생성 (`.gitignore` 처리됨):

```yaml
spring:
  datasource:
    url: jdbc:mariadb://<host>:3306/<database>
    username: <username>
    password: <password>
app:
  jwt:
    secret: <32바이트 이상의 비밀키>
```

**(B) 환경변수**:

```bash
export DB_URL="jdbc:mariadb://<host>:3306/<database>"
export DB_USERNAME="<username>"
export DB_PASSWORD="<password>"
export JWT_SECRET="<32바이트 이상의 비밀키>"
```

> 스키마는 수동 관리한다 (`ddl-auto: none`). `user_tb`, `board_tb` 테이블이 미리 준비돼 있어야 한다.

### 2. 빌드 & 실행

```bash
./gradlew bootRun        # 개발 실행
./gradlew build          # 빌드 (jar 생성)
./gradlew test           # 테스트
```

기본 포트는 `8080`. CORS 는 `http://localhost:5173` (프론트엔드) 허용.

## 인증

- 로그인/회원가입 시 JWT 를 발급한다 (기본 만료 1일).
- 보호된 API 호출 시 헤더에 토큰을 담는다.

```
Authorization: Bearer <token>
```

## API 요약

### 인증 (`/api/auth`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/signup` | 회원가입 + 토큰 발급 | ❌ |
| POST | `/api/auth/login` | 로그인 + 토큰 발급 | ❌ |
| GET | `/api/auth/me` | 내 정보 조회 | ✅ |

### 회원 (`/api/users`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/users?keyword=&page=&size=` | 목록/검색(이름·이메일) | ❌ |
| GET | `/api/users/{id}` | 단건 조회 | ❌ |
| POST | `/api/users` | 회원 등록 | ✅ |
| PUT | `/api/users/{id}` | 회원 수정 (본인만) | ✅ |
| DELETE | `/api/users/{id}` | 회원 삭제 (본인만) | ✅ |

### 게시글 (`/api/boards`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/boards?type=&keyword=&page=&size=` | 목록/검색 (`type`: title·content·all·author) | ❌ |
| GET | `/api/boards/{id}` | 단건 조회 | ❌ |
| POST | `/api/boards` | 게시글 작성 (multipart) | ✅ |
| PUT | `/api/boards/{id}` | 게시글 수정 (작성자만, multipart) | ✅ |
| DELETE | `/api/boards/{id}` | 게시글 삭제 (작성자만) | ✅ |

게시글 작성/수정은 `multipart/form-data` 로 `title`, `content`, `image`(선택) 를 보낸다. 작성자는 JWT 에서 결정된다.

### 요청 예시

```bash
# 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"a@b.com","name":"홍길동","password":"1234"}'

# 게시글 작성 (이미지 포함)
curl -X POST http://localhost:8080/api/boards \
  -H "Authorization: Bearer <token>" \
  -F "title=제목" -F "content=내용" -F "image=@photo.png"
```

## 보안 특징

- 비밀번호 BCrypt 해싱, 응답에서 비밀번호 제외
- 로그인 실패 메시지 동일화 (계정 열거 방지)
- 게시글·회원 수정/삭제 시 소유자 검증 (IDOR 방지) → 권한 없으면 `403`
- 업로드 파일 확장자/Content-Type 화이트리스트 (`jpg`, `jpeg`, `png`, `gif`, `webp`), 파일명 UUID 화 (경로 순회·저장형 XSS 방지)
- 비밀정보는 환경변수/로컬 파일로 분리해 git 노출 차단

## 업로드 파일

- 저장 위치: `uploads/` (git 제외)
- 접근 경로: `/uploads/{filename}` (정적 서빙)
- 최대 크기: 5MB
