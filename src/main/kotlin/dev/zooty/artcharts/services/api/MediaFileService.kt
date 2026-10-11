package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class MediaFile(
    val resource: Resource,
    val mediaType: MediaType,
    val fileName: String,
)

@Service
class MediaFileService(
    private val artRepository: ArtRepository,
    @Value($$"${artcharts.media-root:.}") mediaRoot: String
) {
    private val root: Path = Paths.get(mediaRoot).toAbsolutePath().normalize()

    fun resolveForArt(id: Long): List<MediaFile> {
        val art = artRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Art with id $id not found") }
        val visibilityDirectory = if (art.isNsfw) "NSFW" else "SFW"
        val year = art.deliveredDate.take(4)
        val candidate = root.resolve(visibilityDirectory).resolve(year).resolve(art.fileName).normalize()
        return when {
            isRegularFile(candidate) -> listOf(createMediaFile(candidate))
            isFolder(candidate) -> gatherSubMediaFiles(candidate).ifEmpty { listOf(placeholder()) }
            else -> listOf(placeholder())
        }
    }

    fun saveFile(file: MultipartFile, fileName: String, date: String, isNsfw: Boolean) {
        saveFiles(listOf(file), fileName, date, isNsfw)
    }

    fun saveFiles(files: List<MultipartFile>, fileName: String, date: String, isNsfw: Boolean) {
        require(files.isNotEmpty()) { "At least one file must be provided" }
        val basePath = resolveBasePath(fileName, date, isNsfw)
        require(basePath.startsWith(root)) { "File path must be inside the media root" }

        if (files.size == 1) 
            saveSingleFile(files.single(), basePath)
        else 
            saveMultipleFiles(files, basePath)
    }

    private fun resolveBasePath(fileName: String, date: String, isNsfw: Boolean): Path {
        val visibilityDirectory = if (isNsfw) "NSFW" else "SFW"
        val year = date.take(4)
        val safeName = safePathPart(fileName, "File name must not be blank")
        return root.resolve(visibilityDirectory).resolve(year).resolve(safeName).normalize()
    }

    private fun saveSingleFile(file: MultipartFile, basePath: Path) {
        Files.createDirectories(basePath.parent)
        file.transferTo(basePath)
    }

    private fun saveMultipleFiles(
        files: List<MultipartFile>,
        basePath: Path
    ) {
        Files.createDirectories(basePath)
        files.forEach { file ->
            val originalFileName = file.originalFilename
                ?: throw IllegalArgumentException("Uploaded file must have a name")
            val safeFileName = safePathPart(originalFileName, "Uploaded file name must not be blank")
            val pathToSave = basePath.resolve(safeFileName).normalize()
            require(pathToSave.parent == basePath) { "Uploaded file path must be inside the media folder" }
            file.transferTo(pathToSave)
        }
    }

    private fun safePathPart(value: String, errorMessage: String): String =
        Paths.get(value).fileName?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException(errorMessage)

    private fun gatherSubMediaFiles(folderPath: Path): List<MediaFile> {
        return Files.walk(folderPath, 1).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .sorted(compareBy { it.fileName.toString() })
                .map { createMediaFile(it) }
                .toList()
        }
    }

    private fun isRegularFile(path: Path): Boolean {
        return path.startsWith(root) && Files.isRegularFile(path)
    }

    private fun isFolder(path: Path): Boolean {
        return path.startsWith(root) && Files.isDirectory(path)
    }

    private fun createMediaFile(path: Path): MediaFile {
        return MediaFile(
            FileSystemResource(path),
            contentType(path),
            path.fileName.toString(),
        )
    }

    private fun placeholder(): MediaFile = MediaFile(
        ClassPathResource("static/site/placeholder.svg"),
        MediaType("image", "svg+xml"),
        "placeholder.svg",
    )

    private fun contentType(path: Path): MediaType = runCatching {
        MediaType.parseMediaType(Files.probeContentType(path) ?: "application/octet-stream")
    }.getOrDefault(MediaType.APPLICATION_OCTET_STREAM)
}
