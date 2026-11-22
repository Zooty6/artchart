package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.dto.CreateWipRequest
import dev.zooty.artcharts.dto.DeliverArtRequest
import dev.zooty.artcharts.dto.UpdateWipRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.WipRepository
import dev.zooty.artcharts.persistence.entity.Wip
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockMultipartFile
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@ExtendWith(MockitoExtension::class)
class WipServiceTest {
    @Mock
    lateinit var wipRepository: WipRepository

    @Mock
    lateinit var artistRepository: ArtistRepository

    @Mock
    lateinit var artCreationService: ArtCreationService

    @InjectMocks
    lateinit var service: WipService

    @Test
    fun `returns all wips`() {
        val wips = listOf(TestFixtures.wip())
        `when`(wipRepository.findAll()).thenReturn(wips)

        assertEquals(wips, service.getWips())
    }

    @Test
    fun `returns wip by id`() {
        val wip = TestFixtures.wip()
        `when`(wipRepository.findById(1L)).thenReturn(Optional.of(wip))

        assertSame(wip, service.getWip(1L))
    }

    @Test
    fun `missing wip fails get update and delivery`() {
        `when`(wipRepository.findById(1L)).thenReturn(Optional.empty())

        assertFailsWith<ResourceNotFoundException> { service.getWip(1L) }
        assertFailsWith<ResourceNotFoundException> { service.update(1L, UpdateWipRequest()) }
        assertFailsWith<ResourceNotFoundException> { service.deliverArt(1L, DeliverArtRequest(), emptyList()) }

        verify(wipRepository, never()).delete(any(Wip::class.java))
        verifyNoInteractions(artCreationService)
    }

    @Test
    fun `creates wip using trimmed artist name`() {
        val artist = TestFixtures.artist()
        val saved = TestFixtures.wip()
        `when`(artistRepository.findByNameIgnoreCase("Artist")).thenReturn(artist)
        `when`(wipRepository.save(any(Wip::class.java))).thenReturn(saved)

        assertSame(saved, service.create(CreateWipRequest(type = " commission ", species = " cat ", artistName = " Artist ")))

        val captor = ArgumentCaptor.forClass(Wip::class.java)
        verify(wipRepository).save(captor.capture())
        assertEquals("commission", captor.value.type)
        assertEquals("cat", captor.value.species)
        assertSame(artist, captor.value.artist)
    }

    @Test
    fun `unknown artist prevents creation`() {
        assertFailsWith<ResourceNotFoundException> {
            service.create(CreateWipRequest(type = "commission", species = "cat", artistName = "Missing"))
        }

        verifyNoInteractions(wipRepository, artCreationService)
    }

    @Test
    fun `updates existing wip`() {
        val wip = TestFixtures.wip()
        `when`(wipRepository.findById(1L)).thenReturn(Optional.of(wip))

        service.update(1L, UpdateWipRequest(type = " sketch ", species = " fox ", amount = 25.0, isNsfw = true))

        assertEquals("sketch", wip.type)
        assertEquals("fox", wip.species)
        assertEquals(25.0, wip.price.amount)
        assertEquals(true, wip.isNsfw)
    }

    @Test
    fun `delivery forwards inherited fields and files and removes wip`() {
        val wip = TestFixtures.wip(isNsfw = true)
        val art = TestFixtures.art()
        val files = listOf(MockMultipartFile("file", "art.png", "image/png", byteArrayOf(1)))
        `when`(wipRepository.findById(1L)).thenReturn(Optional.of(wip))
        val request = DeliverArtRequest(deliveredDate = "2024-02-01", fileName = "art")
        val expected = CreateArtRequest(
            type = "commission", otherCharacters = "Fox", quality = "High", species = "cat",
            orderedDate = "2024-01-01", payedDate = "2024-01-02", deliveredDate = "2024-02-01",
            fileName = "art", currency = wip.price.currency, amount = 10.0, note = "Note",
            artistName = "Artist", isNsfw = true,
        )
        `when`(artCreationService.create(expected, files)).thenReturn(art)

        assertSame(art, service.deliverArt(1L, request, files))

        verify(artCreationService).create(expected, files)
        verify(wipRepository).delete(wip)
    }
}
