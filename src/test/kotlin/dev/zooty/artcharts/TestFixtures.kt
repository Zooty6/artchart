package dev.zooty.artcharts

import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price

object TestFixtures {
    fun artist(name: String = "Artist") = Artist(
        1L, name, null, null, null, null, null, null, null, null, null, null, null
    )

    fun art(
        id: Long = 1L,
        year: Int = 2024,
        isNsfw: Boolean = false,
        fileName: String = "art.png",
        artist: Artist = artist(),
    ) = Art(
        id = id,
        otherCharacters = null,
        type = "commission",
        quality = null,
        species = "cat",
        orderedDate = null,
        payedDate = null,
        deliveredDate = "$year-01-01",
        fileName = fileName,
        price = Price(Currency.USD, 10.0),
        note = null,
        artist = artist,
        isNsfw = isNsfw,
        link = "https://example.com",
    )
}
