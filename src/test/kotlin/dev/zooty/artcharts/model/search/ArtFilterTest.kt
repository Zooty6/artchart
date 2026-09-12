package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Tag
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ArtFilterTest {

    @Test
    fun `factory creates every supported filter`() {
        val values = mapOf(
            "tag" to TagArtFilter::class,
            "artist" to ArtistArtFilter::class,
            "otherCharacters" to OtherCharacterArtFilter::class,
            "type" to TypeArtFilter::class,
            "quality" to QualityArtFilter::class,
            "nsfw" to NsfwArtFilter::class,
            "deliveredDate" to DeliveryDateArtFilter::class,
            "paidDate" to PaidDateArtFilter::class,
            "orderedDate" to OrderDateArtFilter::class,
        )

        values.forEach { (name, type) ->
            assertEquals(type, ArtFilter.createFilter("$name:value")::class)
        }
    }

    @Test
    fun `factory rejects malformed and unknown filters`() {
        assertThrows(IllegalArgumentException::class.java) { ArtFilter.createFilter("tag") }
        assertThrows(IllegalArgumentException::class.java) { ArtFilter.createFilter("unknown:value") }
        assertThrows(IllegalArgumentException::class.java) { ArtFilter.createFilter("tag:one:two") }
    }

    @Test
    fun `text filters are case insensitive and support partial matches`() {
        val art = art(
            artistName = "Blue Fox",
            otherCharacters = "Red Panda",
            type = "Digital Commission",
            quality = "High Quality",
            tags = mutableSetOf(Tag("Cute-Furry", "style")),
        )

        assertTrue(ArtistArtFilter("fox").filter(art))
        assertTrue(OtherCharacterArtFilter("panda").filter(art))
        assertTrue(TypeArtFilter("commission").filter(art))
        assertTrue(QualityArtFilter("quality").filter(art))
        assertTrue(TagArtFilter("furry").filter(art))
        assertFalse(OtherCharacterArtFilter("dragon").filter(art))
        assertFalse(QualityArtFilter("low").filter(art))
    }

    @Test
    fun `nullable text filters reject missing values`() {
        val art = art(otherCharacters = null, quality = null)

        assertFalse(OtherCharacterArtFilter("anything").filter(art))
        assertFalse(QualityArtFilter("anything").filter(art))
    }

    @Test
    fun `nsfw filter compares boolean values`() {
        assertTrue(NsfwArtFilter("true").filter(art(isNsfw = true)))
        assertFalse(NsfwArtFilter("true").filter(art(isNsfw = false)))
        assertTrue(NsfwArtFilter("false").filter(art(isNsfw = false)))
    }

    @Test
    fun `date filters support equality year and all relation operators`() {
        val art = art(
            orderedDate = "2023-05-10",
            payedDate = "2024-06-15",
            deliveredDate = "2024-01-20",
        )

        assertTrue(DeliveryDateArtFilter(">=2024-01-20").filter(art))
        assertTrue(DeliveryDateArtFilter("<2025-01-01").filter(art))
        assertTrue(DeliveryDateArtFilter("=2024").filter(art))
        assertTrue(DeliveryDateArtFilter("2024").filter(art))
        assertTrue(PaidDateArtFilter("<=2024-06-15").filter(art))
        assertTrue(OrderDateArtFilter("=>2023").filter(art))
        assertFalse(OrderDateArtFilter(">2024").filter(art))
    }

    @Test
    fun `date filters reject null dates`() {
        val art = art(orderedDate = null, payedDate = null)

        assertFalse(OrderDateArtFilter("2024").filter(art))
        assertFalse(PaidDateArtFilter("2024").filter(art))
    }

    private fun art(
        artistName: String = "Artist",
        otherCharacters: String? = "Other",
        type: String = "commission",
        quality: String? = "High",
        orderedDate: String? = "2024-01-01",
        payedDate: String? = "2024-01-10",
        deliveredDate: String = "2024-01-20",
        isNsfw: Boolean = false,
        tags: MutableSet<Tag> = mutableSetOf(),
    ): Art = TestFixtures.art(
        artist = TestFixtures.artist(artistName),
        isNsfw = isNsfw,
    ).let { original ->
        Art(
            id = original.id,
            otherCharacters = otherCharacters,
            type = type,
            quality = quality,
            species = original.species,
            orderedDate = orderedDate,
            payedDate = payedDate,
            deliveredDate = deliveredDate,
            fileName = original.fileName,
            price = original.price,
            note = original.note,
            artist = original.artist,
            isNsfw = original.isNsfw,
            link = original.link,
            tags = tags,
        )
    }
}
