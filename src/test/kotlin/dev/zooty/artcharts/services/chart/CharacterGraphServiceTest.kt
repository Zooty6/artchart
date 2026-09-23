package dev.zooty.artcharts.services.chart

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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(MockitoExtension::class)
class CharacterGraphServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository

    @ParameterizedTest
    @EnumSource(GraphLayout::class)
    fun `characterGraph renders every graph layout`(graphLayout: GraphLayout) {
        `when`(artRepository.findAll()).thenReturn(
            listOf(
                art(otherCharacters = "Alice, Bob"),
                art(otherCharacters = "Alice, Bob")
            )
        )
        val service = CharacterGraphService(SvgConverterService(), artRepository)

        val svg = service.characterGraph(800, 400, graphLayout, false)

        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("Alice"))
        assertTrue(svg.contains("Bob"))
    }

    @Test
    fun `LIST layout renders weighted connections with aligned columns`() {
        `when`(artRepository.findAll()).thenReturn(
            List(10) { art(otherCharacters = "Al, Bob") } +
                List(2) { art(otherCharacters = "LongName, C") }
        )
        val service = CharacterGraphService(SvgConverterService(), artRepository)

        val svg = service.characterGraph(800, 400, GraphLayout.LIST, false)

        assertTrue(svg.contains("Al---10---Bob"))
        assertTrue(svg.contains("C----2----LongName"))
    }

    private fun art(otherCharacters: String?) = Art(
        id = 1,
        otherCharacters = otherCharacters,
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
        isNsfw = false,
        link = "https://example.com",
    )
}
