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
class CharacterGraphServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository

    @Test
    fun `characterGraph includes graph labels for shared characters`() {
        `when`(artRepository.findAll()).thenReturn(
            listOf(
                art(otherCharacters = "Alice, Bob"),
                art(otherCharacters = "Alice, Bob")
            )
        )
        val service = CharacterGraphService(SvgConverterService(), artRepository)

        val svg = service.characterGraph(GraphLayout.CIRCLE, false)

        assertTrue(svg.contains("Alice"))
        assertTrue(svg.contains("Bob"))
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
