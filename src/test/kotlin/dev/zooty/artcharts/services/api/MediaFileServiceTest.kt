package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.ArtRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.test.assertFailsWith

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

        assertEquals(file.toAbsolutePath(), result.single().resource.file.toPath().toAbsolutePath())
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

        assertEquals(file.toAbsolutePath(), result.single().resource.file.toPath().toAbsolutePath())
    }

    @Test
    fun `returns placeholder when file is missing`() {
        `when`(artRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.art()))

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertTrue(result.single().resource.exists())
        assertEquals("image/svg+xml", result.single().mediaType.toString())
    }

    @Test
    fun `does not resolve path outside media root`() {
        val outside = mediaRoot.parent.resolve("outside.png")
        Files.writeString(outside, "image")
        `when`(artRepository.findById(1L)).thenReturn(
            Optional.of(TestFixtures.art(fileName = "../outside.png"))
        )

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertEquals("image/svg+xml", result.single().mediaType.toString())
    }

    @Test
    fun `resolves direct folder files in file name order`() {
        val folder = mediaRoot.resolve("SFW/2024/gallery")
        Files.createDirectories(folder)
        Files.writeString(folder.resolve("02.png"), "second")
        Files.writeString(folder.resolve("01.png"), "first")
        Files.createDirectories(folder.resolve("nested"))
        Files.writeString(folder.resolve("nested/03.png"), "ignored")
        `when`(artRepository.findById(1L)).thenReturn(
            Optional.of(TestFixtures.art(fileName = "gallery"))
        )

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertEquals(listOf("01.png", "02.png"), result.map { it.fileName })
    }

    @Test
    fun `returns placeholder for an empty folder`() {
        val folder = mediaRoot.resolve("SFW/2024/gallery")
        Files.createDirectories(folder)
        `when`(artRepository.findById(1L)).thenReturn(
            Optional.of(TestFixtures.art(fileName = "gallery"))
        )

        val result = MediaFileService(artRepository, mediaRoot.toString()).resolveForArt(1L)

        assertEquals(1, result.size)
        assertEquals("placeholder.svg", result.single().fileName)
    }

    @Test
    fun `saves uploaded SFW file below its delivery year`() {
        val upload = MockMultipartFile("file", "art.png", "image/png", "image".toByteArray())

        MediaFileService(artRepository, mediaRoot.toString())
            .saveFile(upload, "art.png", "2024-01-03", false)

        val saved = mediaRoot.resolve("SFW/2024/art.png")
        assertTrue(Files.isRegularFile(saved))
        assertEquals("image", Files.readString(saved))
    }

    @Test
    fun `saves uploaded NSFW file in the NSFW directory and strips path components`() {
        val upload = MockMultipartFile("file", "art.png", "image/png", "image".toByteArray())

        MediaFileService(artRepository, mediaRoot.toString())
            .saveFile(upload, "nested/art.png", "2024-01-03", true)

        val saved = mediaRoot.resolve("NSFW/2024/art.png")
        assertTrue(Files.isRegularFile(saved))
        assertEquals("image", Files.readString(saved))
        assertTrue(!Files.exists(mediaRoot.resolve("nested/art.png")))
    }

    @Test
    fun `saves multiple uploaded files in a named folder`() {
        val first = MockMultipartFile("file", "01.png", "image/png", "first".toByteArray())
        val second = MockMultipartFile("file", "02.jpg", "image/jpeg", "second".toByteArray())

        MediaFileService(artRepository, mediaRoot.toString())
            .saveFiles(listOf(first, second), "gallery", "2024-01-03", false)

        val folder = mediaRoot.resolve("SFW/2024/gallery")
        assertEquals("first", Files.readString(folder.resolve("01.png")))
        assertEquals("second", Files.readString(folder.resolve("02.jpg")))
    }

    @Test
    fun `strips path components from multiple uploaded file names`() {
        val first = MockMultipartFile("file", "nested/01.png", "image/png", "first".toByteArray())
        val second = MockMultipartFile("file", "nested/02.jpg", "image/jpeg", "second".toByteArray())

        MediaFileService(artRepository, mediaRoot.toString())
            .saveFiles(listOf(first, second), "gallery", "2024-01-03", false)

        assertTrue(Files.isRegularFile(mediaRoot.resolve("SFW/2024/gallery/01.png")))
        assertTrue(Files.isRegularFile(mediaRoot.resolve("SFW/2024/gallery/02.jpg")))
        assertTrue(!Files.exists(mediaRoot.resolve("SFW/2024/gallery/nested")))
    }

    @Test
    fun `rejects uploaded file with a blank file name`() {
        val upload = MockMultipartFile("file", "art.png", "image/png", "image".toByteArray())

        assertFailsWith<IllegalArgumentException> {
            MediaFileService(artRepository, mediaRoot.toString())
                .saveFile(upload, "", "2024-01-03", false)
        }
    }
}
