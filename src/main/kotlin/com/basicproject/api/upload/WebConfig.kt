package com.basicproject.api.upload

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// 업로드 이미지 정적 서빙: GET /uploads/{filename} → {upload-dir}/{filename}
@Configuration
class WebConfig(private val storage: FileStorageService) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:${storage.root}/")
    }
}
