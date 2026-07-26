package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SpeciesDistributionServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository

    @Test
    fun `pie chart uses species counts from repository`() {
        `when`(artRepository.findAll()).thenReturn(
            listOf(
                art(species = "cat"),
                art(species = "cat"),
                art(species = "dog")
            )
        )
        val service = SpeciesDistributionService(SvgConverterService(), artRepository)

        val svg = service.speciesDistribution(500, 400, ChartType.PIE)

        assertTrue(svg.startsWith("<svg"))
    }

    @Test
    fun `treemap renders svg for species distribution`() {
        `when`(artRepository.findAll()).thenReturn(
            listOf(
                art(species = "cat"),
                art(species = "dog")
            )
        )
        val service = SpeciesDistributionService(SvgConverterService(), artRepository)

        val svg = service.speciesDistribution(500, 400, ChartType.TREEMAP)

        assertTrue(svg.startsWith("<svg"))
    }

    private fun art(species: String) = Art(
        id = 1,
        otherCharacters = null,
        type = "commission",
        quality = null,
        species = species,
        orderedDate = null,
        payedDate = null,
        deliveredDate = "2024-01-01",
        fileName = "art.png",
        price = Price(Currency.USD, 10.0),
        note = null,
        artist = Artist(1, "Artist", null, null, null, null, null, null, null, null, null, null, null),
        isNsfw = false,
        link = "https://example.com",
    )
}
