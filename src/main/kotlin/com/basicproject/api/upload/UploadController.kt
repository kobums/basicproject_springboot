package com.basicproject.api.upload

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

// POST /api/upload/index (gomachine README "인증 API 계약", Go 템플릿과 같다). POST /api/upload 도 같은 동작이다.
// 로그인 필요, multipart/form-data, 파일 필드 이름 "upload". 응답 200 {filename, url}, 실패 400 / 401.
// filename 을 테이블 컬럼(예: board.img)에 저장하고, 화면에서는 url (/uploads/{filename}) 로 읽는다.
@RestController
@RequestMapping("/api/upload")
class UploadController(private val storage: FileStorageService) {

    @PostMapping(path = ["/index", ""], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestParam("upload", required = false) file: MultipartFile?): Map<String, String> {
        val filename = storage.store(file)
        return mapOf(
            "filename" to filename,
            "url" to "/uploads/$filename",
        )
    }
}
