package com.basicproject.api.upload

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// 업로드 이미지를 {upload-dir}/{yyyyMMdd}/{uuid}.{ext} 로 저장한다. /uploads/** 로 정적 서빙되므로
// 이미지(jpg, jpeg, png, gif, webp)만 받는다: 확장자 화이트리스트 + 파일 앞부분(시그니처) 검사. 파일 이름은 서버가 만든다.
@Service
class FileStorageService(@Value("\${app.upload-dir:uploads}") uploadDir: String) {

    val root: Path = Paths.get(uploadDir).toAbsolutePath().normalize()

    init {
        Files.createDirectories(root)
    }

    private val allowed = mapOf(
        "jpg" to "jpg", "jpeg" to "jpg", "png" to "png", "gif" to "gif", "webp" to "webp",
    )

    // 저장한 상대 경로 (예: 20260102/9f0c....png)
    fun store(file: MultipartFile?): String {
        require(file != null && !file.isEmpty) { "이미지 파일이 필요합니다." }
        val original = file.originalFilename ?: ""
        val ext = original.substringAfterLast('.', "").lowercase()
        require(ext in allowed) { "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp 만 가능)" }
        val head = file.inputStream.use { it.readNBytes(12) }
        require(signature(head) == allowed[ext]) { "이미지 파일만 업로드할 수 있습니다." }

        val dir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val relative = "$dir/${UUID.randomUUID().toString().replace("-", "")}.$ext"
        val target = root.resolve(relative).normalize()
        try {
            Files.createDirectories(target.parent)
            file.inputStream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
        } catch (e: IOException) {
            throw UncheckedIOException("파일 저장 실패: $relative", e)
        }
        return relative
    }

    private fun signature(b: ByteArray): String? {
        fun at(i: Int) = if (i < b.size) b[i].toInt() and 0xff else -1
        return when {
            at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF -> "jpg"
            at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> "png"
            at(0) == 'G'.code && at(1) == 'I'.code && at(2) == 'F'.code && at(3) == '8'.code -> "gif"
            at(0) == 'R'.code && at(1) == 'I'.code && at(2) == 'F'.code && at(3) == 'F'.code &&
                at(8) == 'W'.code && at(9) == 'E'.code && at(10) == 'B'.code && at(11) == 'P'.code -> "webp"
            else -> null
        }
    }
}
