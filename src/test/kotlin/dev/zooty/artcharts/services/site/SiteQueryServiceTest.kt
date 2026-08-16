package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SiteQueryServiceTest {
    @Mock
    lateinit var artRepository: ArtRepository

    @Test
    fun `returns distinct years in descending order`() {
        `when`(artRepository.findAll()).thenReturn(
            listOf(TestFixtures.art(year = 2023), TestFixtures.art(2, 2025), TestFixtures.art(3, 2025))
        )

        assertEquals(listOf(2025, 2023), SiteQueryService(artRepository).years())
    }

    @Test
    fun `filters arts by year and NSFW flag`() {
        `when`(artRepository.findAll()).thenReturn(
            listOf(TestFixtures.art(1, 2024), TestFixtures.art(2, 2024, true), TestFixtures.art(3, 2023))
        )
        val service = SiteQueryService(artRepository)

        assertEquals(2, service.artsForYear(2024).size)
        assertEquals(1, service.artsForYear(2024, true).size)
    }

    @Test
    fun `throws when art does not exist`() {
        `when`(artRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) { SiteQueryService(artRepository).art(99L) }
    }
}
