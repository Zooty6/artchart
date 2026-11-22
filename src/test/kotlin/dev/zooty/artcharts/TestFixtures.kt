package dev.zooty.artcharts

import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import dev.zooty.artcharts.persistence.entity.Tag
import dev.zooty.artcharts.persistence.entity.Wip

object TestFixtures {
    fun artist(name: String = "Artist") = Artist(
        1L, name, null, null, null, null, null, null, null, null, null, null, null, null, null
    )

    fun wip(id: Long = 1L, artist: Artist = artist(), isNsfw: Boolean = false) = Wip(
        id, "Fox", "commission", "High", "cat", "2024-01-01", "2024-01-02",
        Price(Currency.USD, 10.0), "Note", artist, isNsfw,
    )

    fun art(
        id: Long = 1L,
        year: Int = 2024,
        isNsfw: Boolean = false,
        fileName: String = "art.png",
        artist: Artist = artist(),
        tags: MutableSet<Tag> = mutableSetOf(),
    ) = Art(
        id = id,
        otherCharacters = null,
        type = "commission",
        quality = null,
        species = "cat",
        orderedDate = "$year-01-01",
        payedDate = "$year-01-02",
        deliveredDate = "$year-01-03",
        fileName = fileName,
        price = Price(Currency.USD, 10.0),
        note = null,
        artist = artist,
        isNsfw = isNsfw,
        link = "https://example.com",
        tags = tags,
    )
}
