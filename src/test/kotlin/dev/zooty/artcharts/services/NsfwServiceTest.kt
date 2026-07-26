package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import org.jfree.chart.ChartFactory
import org.jfree.data.general.DefaultPieDataset
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class NsfwServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository

    @Test
    fun `nsfwRatio builds a pie chart with sfw and nsfw counts`() {
        `when`(artRepository.findAll()).thenReturn(listOf(art(false), art(true), art(true)))
        val service = NsfwService(SvgConverterService(), artRepository)

        val chartField = NsfwService::class.java.getDeclaredMethod("nsfwRatio", Int::class.java, Int::class.java)
        val svg = service.nsfwRatio(640, 480)

        assertTrue(svg.startsWith("<svg"))
    }

    private fun art(isNsfw: Boolean) = Art(
        id = 1,
        otherCharacters = null,
        type = "commission",
        quality = null,
        species = "cat",
        orderedDate = null,
        payedDate = null,
        deliveredDate = "2024-01-01",
        fileName = "art.png",
        price = Price(Currency.USD, 10.0),
        note = null,
        artist = Artist(1, "Artist", null, null, null, null, null, null, null, null, null, null, null),
        isNsfw = isNsfw,
        link = "https://example.com",
    )
}
