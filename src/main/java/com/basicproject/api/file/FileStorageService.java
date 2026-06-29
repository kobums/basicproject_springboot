package com.basicproject.api.file;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

// 업로드 파일을 로컬 디렉터리에 저장하고 저장 파일명을 돌려준다.
@Service
public class FileStorageService {

    // /uploads/** 가 정적 서빙되므로 이미지 외 파일(.html, .svg 등)은 저장형 XSS 위험 → 화이트리스트로 제한.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 디렉터리 생성 실패: " + root, e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase();
        }
        // 확장자 + Content-Type 둘 다 이미지 화이트리스트에 있어야 저장 허용.
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp 만 가능)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + filename, e);
        }
        return filename;
    }

    // 업로드된 파일만 삭제 (외부 URL/경로는 건드리지 않음)
    public void deleteQuietly(String img) {
        if (img == null || img.isBlank() || img.startsWith("http://")
                || img.startsWith("https://") || img.startsWith("/")) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(img));
        } catch (IOException ignored) {
            // 파일이 없거나 삭제 실패해도 무시
        }
    }

    public Path getRoot() {
        return root;
    }
}
