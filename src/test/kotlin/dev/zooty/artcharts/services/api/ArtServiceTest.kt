package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.TagRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import dev.zooty.artcharts.persistence.entity.Tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ArtServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository
    @Mock
    lateinit var tagRepository: TagRepository

    @Test
    fun `addTag attaches existing tag`() {
        val art = art()
        val tag = Tag("cute", "style")
        `when`(artRepository.findById(1L)).thenReturn(Optional.of(art))
        `when`(tagRepository.findByName("cute")).thenReturn(Optional.of(tag))
        val service = ArtService(artRepository, tagRepository)

        service.addTag(1L, TagDto("cute", "style"))

        assertEquals(1, art.tags.size)
        assertEquals("cute", art.tags.first().name)
    }

    @Test
    fun `addTag creates missing tag`() {
        val art = art()
        `when`(artRepository.findById(1L)).thenReturn(Optional.of(art))
        `when`(tagRepository.findByName("new-tag")).thenReturn(Optional.empty())
        `when`(tagRepository.save(any(Tag::class.java))).thenAnswer { it.arguments[0] as Tag }
        val service = ArtService(artRepository, tagRepository)

        service.addTag(1L, TagDto("new-tag", "misc"))

        assertEquals(1, art.tags.size)
        assertEquals("new-tag", art.tags.first().name)
        assertEquals("misc", art.tags.first().category)
    }

    @Test
    fun `addTag throws when art is missing`() {
        `when`(artRepository.findById(99L)).thenReturn(Optional.empty())
        val service = ArtService(artRepository, tagRepository)

        assertThrows(ResourceNotFoundException::class.java) {
            service.addTag(99L, TagDto("tag", "cat"))
        }
    }

    private fun art() = Art(
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
        isNsfw = false,
        link = "https://example.com",
    )
}
