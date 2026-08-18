package dev.zooty.artcharts.dto

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.entity.Currency
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class CreateRequestTest {
    @Test
    fun `blank optional artist fields are converted to null`() {
        val artist = CreateArtistRequest(
            name = "Artist",
            furaffinity = " ",
            twitter = "",
            note = "  ",
            vgen = "vgen.example",
        ).toEntity()

        assertNull(artist.furaffinity)
        assertNull(artist.twitter)
        assertNull(artist.note)
        assertEquals("vgen.example", artist.vgen)
    }

    @Test
    fun `blank optional art fields are converted to null`() {
        val art = CreateArtRequest(
            type = "commission",
            species = "cat",
            deliveredDate = "2024-01-01",
            fileName = "art.png",
            currency = Currency.USD,
            otherCharacters = " ",
            quality = "",
            orderedDate = "  ",
            payedDate = "",
            note = " ",
            link = "",
            artistName = "Artist",
        ).toEntity(TestFixtures.artist())

        assertNull(art.otherCharacters)
        assertNull(art.quality)
        assertNull(art.orderedDate)
        assertNull(art.payedDate)
        assertNull(art.note)
        assertNull(art.link)
    }
}
