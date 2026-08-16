package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.ArtRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MediaFileServiceTest {
    @Mock
    lateinit var artRepository: ArtRepository

    @TempDir
    lateinit var mediaRoot: Path

    @Test
    fun `resolves SFW file under SFW year directory`() {
        val file = mediaRoot.resolve("SFW/2024/art.png")
        Files.createDirectories(file.parent)
        Files.writeString(file, "image")
        `when`(artRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.art()))

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertEquals(file.toAbsolutePath(), result.resource.file.toPath().toAbsolutePath())
    }

    @Test
    fun `resolves NSFW file under NSFW year directory`() {
        val file = mediaRoot.resolve("NSFW/2024/nsfw.png")
        Files.createDirectories(file.parent)
        Files.writeString(file, "image")
        `when`(artRepository.findById(1L)).thenReturn(
            Optional.of(TestFixtures.art(isNsfw = true, fileName = "nsfw.png"))
        )

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertEquals(file.toAbsolutePath(), result.resource.file.toPath().toAbsolutePath())
    }

    @Test
    fun `returns placeholder when file is missing`() {
        `when`(artRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.art()))

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertTrue(result.resource.exists())
        assertEquals("image/svg+xml", result.mediaType.toString())
    }

    @Test
    fun `does not resolve path outside media root`() {
        val outside = mediaRoot.parent.resolve("outside.png")
        Files.writeString(outside, "image")
        `when`(artRepository.findById(1L)).thenReturn(
            Optional.of(TestFixtures.art(fileName = "../outside.png"))
        )

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertEquals("image/svg+xml", result.mediaType.toString())
    }
}
