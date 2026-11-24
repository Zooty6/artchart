package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.services.api.MediaFileService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

@RestController
@ConditionalOnProperty("spring.ai.mcp.server.enabled", havingValue = "true")
internal class McpMediaDownloadController(private val mediaFileService: MediaFileService) {
    @GetMapping("/site/media/{id}/download")
    fun download(@PathVariable id: Long, @RequestParam fileName: String): ResponseEntity<Resource> {
        val file = mediaFileService.getStoredMedia(id, fileName)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(file.fileName, StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .body(file.resource)
    }
}
