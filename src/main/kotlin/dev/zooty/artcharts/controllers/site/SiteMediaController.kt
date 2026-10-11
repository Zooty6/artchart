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

data class MediaManifest(
    val count: Int,
    val items: List<MediaManifestItem>,
)

data class MediaManifestItem(
    val index: Int,
    val url: String,
    val fileName: String,
    val mediaType: String,
    val kind: String,
)

@Controller
@RequestMapping("/site/media")
class SiteMediaController(
    private val mediaFileService: MediaFileService,
    @param:Value($$"${artcharts.media-cache-max-age-seconds}") private val cacheMaxAgeSeconds: Long,
) {
    @GetMapping("/{id}")
    fun media(@PathVariable id: Long): ResponseEntity<Resource> {
        return mediaResponse(mediaFileService.resolveForArt(id).first())
    }

    @GetMapping("/{id}/{index}")
    fun indexedMedia(@PathVariable id: Long, @PathVariable index: Int): ResponseEntity<Resource> {
        val mediaFiles = mediaFileService.resolveForArt(id)
        if (index !in 1..mediaFiles.size) {
            throw dev.zooty.artcharts.exceptions.ResourceNotFoundException("Media index $index not found for art $id")
        }
        return mediaResponse(mediaFiles[index - 1])
    }

    @GetMapping("/{id}/manifest")
    fun manifest(@PathVariable id: Long): ResponseEntity<MediaManifest> {
        val items = mediaFileService.resolveForArt(id).mapIndexed { index, media ->
            MediaManifestItem(
                index = index + 1,
                url = "/site/media/$id/${index + 1}",
                fileName = media.fileName,
                mediaType = media.mediaType.toString(),
                kind = if (media.mediaType.type == "video") "VIDEO" else "IMAGE",
            )
        }
        return ResponseEntity.ok(MediaManifest(items.size, items))
    }

    private fun mediaResponse(media: dev.zooty.artcharts.services.api.MediaFile): ResponseEntity<Resource> {
        val headers = HttpHeaders()
        headers.contentType = media.mediaType
        headers.contentDisposition = ContentDisposition.inline().build()
        headers.cacheControl = "public, max-age=${cacheMaxAgeSeconds}, immutable"
        return ResponseEntity.ok().headers(headers).body(media.resource)
    }
}
