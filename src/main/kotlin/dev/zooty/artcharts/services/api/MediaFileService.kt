package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
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

    @Synchronized
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

    @Synchronized
    fun saveFiles(files: List<MultipartFile>, fileName: String, date: String, isNsfw: Boolean) {
        require(files.isNotEmpty()) { "At least one file must be provided" }
        val basePath = resolveBasePath(fileName, date, isNsfw)
        require(basePath.startsWith(root)) { "File path must be inside the media root" }

        if (files.size == 1) 
            saveSingleFile(files.single(), basePath)
        else 
            saveMultipleFiles(files, basePath)
    }

    @Synchronized
    fun listStoredMedia(art: Art): List<MediaFile> = storedFiles(checkedMediaPath(art)).map(::createMediaFile)

    @Synchronized
    fun getStoredMedia(id: Long, fileName: String): MediaFile {
        val art = artRepository.findById(id).orElseThrow { ResourceNotFoundException("Artwork not found") }
        return listStoredMedia(art).firstOrNull { it.fileName == fileName }
            ?: throw ResourceNotFoundException("Media file not found")
    }

    @Synchronized
    fun deleteStoredMedia(art: Art, fileName: String) {
        validateMediaName(fileName)
        val target = writableMediaPath(art)
        val file = storedFiles(target).firstOrNull { it.fileName.toString() == fileName }
            ?: throw ResourceNotFoundException("Media file not found")
        Files.delete(file)
    }

    fun validateLocation(fileName: String, date: String) {
        validateMediaName(fileName)
        require(date.take(4).matches(Regex("[0-9]{4}"))) { "Invalid delivery year" }
    }

    fun validateMediaName(name: String) {
        require(name.isNotBlank() && name != "." && name != ".." && name.trimEnd('.', ' ') == name &&
                name.none { it.isISOControl() || it in "\\/:*?\"<>|" }) { "Invalid media file name" }
        require(!name.substringBefore('.').uppercase().matches(Regex("CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³]"))) {
            "Invalid media file name"
        }
    }

    private fun writableMediaPath(art: Art): Path {
        val target = checkedMediaPath(art)
        require(!artRepository.existsByIdNotAndFileNameIgnoreCaseAndIsNsfwAndDeliveredDateStartingWith(
            art.id, art.fileName, art.isNsfw, art.deliveredDate.take(4),
        )) { "Media path is shared by multiple artworks" }
        return target
    }

    private fun checkedMediaPath(art: Art): Path {
        validateLocation(art.fileName, art.deliveredDate)
        val target = resolveBasePath(art.fileName, art.deliveredDate, art.isNsfw)
        val realRoot = if (Files.exists(root)) root.toRealPath() else root
        var path = root
        for (segment in root.relativize(target)) {
            path = path.resolve(segment)
            require(!Files.isSymbolicLink(path)) { "Symbolic links are unsupported in media paths" }
            if (Files.exists(path, NOFOLLOW_LINKS)) {
                require(path.toRealPath().startsWith(realRoot)) { "Media path must stay inside the media root" }
            }
        }
        return target
    }

    private fun storedFiles(path: Path): List<Path> = when {
        Files.isRegularFile(path, NOFOLLOW_LINKS) -> listOf(path)
        Files.isDirectory(path, NOFOLLOW_LINKS) -> Files.list(path).use { paths ->
            paths.sorted(compareBy { it.fileName.toString() }).toList().also { files ->
                require(files.all { Files.isRegularFile(it, NOFOLLOW_LINKS) }) { "Unsupported media layout" }
            }
        }
        Files.notExists(path, NOFOLLOW_LINKS) -> emptyList()
        else -> throw IllegalArgumentException("Unsupported media layout")
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
