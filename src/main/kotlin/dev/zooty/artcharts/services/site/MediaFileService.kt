package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class MediaFile(val resource: Resource, val mediaType: MediaType)

@Service
class MediaFileService(
    private val artRepository: ArtRepository,
    @Value("\${artcharts.media-root:.}") mediaRoot: String,
) {
    private val root: Path = Paths.get(mediaRoot).toAbsolutePath().normalize()

    fun resolveForArt(id: Long): MediaFile {
        val art = artRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Art with id $id not found") }
        val visibilityDirectory = if (art.isNsfw) "NSFW" else "SFW"
        val year = art.deliveredDate.take(4)
        val candidate = root.resolve(visibilityDirectory).resolve(year).resolve(art.fileName).normalize()
        val safeCandidate = if (candidate.startsWith(root) && Files.isRegularFile(candidate)) candidate else null
        return if (safeCandidate == null) placeholder() else MediaFile(
            FileSystemResource(safeCandidate),
            contentType(safeCandidate)
        )
    }

    private fun placeholder() = MediaFile(
        ClassPathResource("static/site/placeholder.svg"),
        MediaType("image", "svg+xml")
    )

    private fun contentType(path: Path): MediaType = runCatching {
        MediaType.parseMediaType(Files.probeContentType(path) ?: "application/octet-stream")
    }.getOrDefault(MediaType.APPLICATION_OCTET_STREAM)
}
