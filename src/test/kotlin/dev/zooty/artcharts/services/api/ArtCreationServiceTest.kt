package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockMultipartFile

@ExtendWith(MockitoExtension::class)
class ArtCreationServiceTest {
    @Mock
    lateinit var artRepository: ArtRepository

    @Mock
    lateinit var artistRepository: ArtistRepository

    @Mock
    lateinit var mediaFileService: MediaFileService

    @Test
    fun `creates art and saves multiple files as one media collection`() {
        val request = request()
        val artist = TestFixtures.artist()
        val createdArt = TestFixtures.art(fileName = "gallery")
        val files = listOf(
            MockMultipartFile("file", "01.png", "image/png", "first".toByteArray()),
            MockMultipartFile("file", "02.png", "image/png", "second".toByteArray()),
        )
        `when`(artistRepository.findByNameIgnoreCase("Artist")).thenReturn(artist)
        `when`(artRepository.save(org.mockito.ArgumentMatchers.any(Art::class.java))).thenReturn(createdArt)

        val result = ArtCreationService(artRepository, artistRepository, mediaFileService)
            .create(request, files)

        assertEquals(createdArt, result)
        verify(mediaFileService).saveFiles(files, "gallery", "2024-01-03", false)
        verify(artRepository).save(org.mockito.ArgumentMatchers.any(Art::class.java))
    }

    @Test
    fun `does not save media when no files are supplied`() {
        val request = request()
        val artist = TestFixtures.artist()
        val createdArt = TestFixtures.art()
        `when`(artistRepository.findByNameIgnoreCase("Artist")).thenReturn(artist)
        `when`(artRepository.save(org.mockito.ArgumentMatchers.any(Art::class.java))).thenReturn(createdArt)

        ArtCreationService(artRepository, artistRepository, mediaFileService)
            .create(request, emptyList())

        verifyNoInteractions(mediaFileService)
    }

    private fun request() = CreateArtRequest(
        type = "commission",
        species = "cat",
        deliveredDate = "2024-01-03",
        fileName = "gallery",
        currency = Currency.USD,
        amount = 10.0,
        artistName = "Artist",
    )
}
