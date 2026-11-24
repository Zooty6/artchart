package dev.zooty.artcharts.mcp

import dev.zooty.artcharts.dto.UpdateArtRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.services.api.MediaFileService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.Base64
import java.util.Locale

@Component
@ConditionalOnProperty("spring.ai.mcp.server.enabled", havingValue = "true")
internal class McpMedia(
    private val arts: ArtRepository,
    private val files: MediaFileService,
    private val links: McpPublicLinks,
) {
    data class Upload(@field:NotBlank val fileName: String, @field:NotBlank val base64: String)
    data class Deletion(@field:Positive val id: Long, @field:NotBlank val fileName: String)
    data class Link(val index: Int, val fileName: String, val mediaType: String, val url: String)

    fun list(id: Long): List<Link> = withMediaLock { links(art(id)) }

    fun multipartFiles(uploads: List<Upload>, artworkFileName: String): List<MultipartFile> {
        if (uploads.size == 1) validateExtension(artworkFileName)
        var remaining = MAX_MEDIA_BYTES
        val names = mutableSetOf<String>()
        return uploads.map { file ->
            files.validateMediaName(file.fileName)
            require(names.add(file.fileName.lowercase(Locale.ROOT))) { "Duplicate media file name" }
            validateExtension(file.fileName)
            require(file.base64.length <= ((remaining + 2) / 3) * 4) { "Media exceeds the 8 MiB limit" }
            val bytes = Base64.getDecoder().decode(file.base64)
            require(bytes.isNotEmpty()) { "Media file must not be empty" }
            remaining -= bytes.size
            require(remaining >= 0) { "Media exceeds the 8 MiB limit" }
            UploadPart(file.fileName, bytes)
        }
    }

    private fun validateExtension(fileName: String) {
        require(fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) in supportedExtensions) {
            "Unsupported media file extension"
        }
    }

    fun delete(request: Deletion): List<Link> = withMediaLock {
        val art = art(request.id)
        files.deleteStoredMedia(art, request.fileName)
        links(art)
    }

    fun <T> withMediaLock(action: () -> T): T = synchronized(files, action)

    fun validateNewLocation(fileName: String, deliveredDate: String) =
        files.validateLocation(fileName.trim(), deliveredDate.trim())

    fun validateMove(art: Art, request: UpdateArtRequest) {
        val date = request.deliveredDate.trim()
        if (art.isNsfw != request.isNsfw || art.deliveredDate.take(4) != date.take(4)) {
            validateNewLocation(art.fileName, date)
            require(files.listStoredMedia(art).isEmpty()) { "Delete or relocate the stored media before changing its year or visibility" }
        }
    }

    private fun art(id: Long): Art = arts.findById(id).orElseThrow { ResourceNotFoundException("Artwork not found") }

    private fun links(art: Art): List<Link> = files.listStoredMedia(art).mapIndexed { offset, file ->
        val index = offset + 1
        Link(index, file.fileName, file.mediaType.toString(),
            links.url("/site/media/${art.id}/download", mapOf("fileName" to file.fileName)))
    }

    private companion object {
        const val MAX_MEDIA_BYTES = 8 * 1024 * 1024
        val supportedExtensions = setOf(
            "png", "jpg", "jpeg", "gif", "webp", "avif", "bmp", "tif", "tiff",
            "mp4", "m4v", "mov", "webm", "mkv", "blend", "psd", "kra", "clip",
        )
    }
}

private class UploadPart(private val fileName: String, private val bytes: ByteArray) : MultipartFile {
    override fun getName() = "files"
    override fun getOriginalFilename() = fileName
    override fun getContentType() = MediaType.APPLICATION_OCTET_STREAM_VALUE
    override fun isEmpty() = bytes.isEmpty()
    override fun getSize() = bytes.size.toLong()
    override fun getBytes() = bytes
    override fun getInputStream() = bytes.inputStream()
    override fun transferTo(dest: File) = transferTo(dest.toPath())
}
