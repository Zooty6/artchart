package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.services.api.MediaFileService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/site/media")
class SiteMediaController(
    private val mediaFileService: MediaFileService,
    @param:Value($$"${artcharts.media-cache-max-age-seconds}") private val cacheMaxAgeSeconds: Long,
) {
    @GetMapping("/{id}")
    fun media(@PathVariable id: Long): ResponseEntity<Resource> {
        val media = mediaFileService.resolveForArt(id)
        val headers = HttpHeaders()
        headers.contentType = media.mediaType
        headers.contentDisposition = ContentDisposition.inline().build()
        headers.cacheControl = "public, max-age=${cacheMaxAgeSeconds}, immutable"
        return ResponseEntity.ok().headers(headers).body(media.resource)
    }
}
