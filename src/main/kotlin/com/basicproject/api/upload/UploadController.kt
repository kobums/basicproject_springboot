package com.basicproject.api.upload

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartHttpServletRequest

// POST /api/upload (= Go 템플릿의 /api/upload/index), 로그인 필요, multipart/form-data.
// 파일 필드 이름은 form 값 name (기본 "upload"). 응답 {filename, original, url}.
// filename 을 테이블 컬럼(예: board.img)에 저장하고, 화면에서는 url (/uploads/{filename}) 로 읽는다.
@RestController
@RequestMapping("/api/upload")
class UploadController(private val storage: FileStorageService) {

    @PostMapping(path = ["", "/index"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        request: MultipartHttpServletRequest,
        @RequestParam("name", required = false, defaultValue = "upload") name: String,
    ): Map<String, String> {
        val file = request.getFile(name)
        val filename = storage.store(file)
        return mapOf(
            "filename" to filename,
            "original" to (file?.originalFilename ?: ""),
            "url" to "/uploads/$filename",
        )
    }
}
