package dev.zooty.artcharts.dto

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.entity.Currency
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WipRequestTest {
    @Test
    fun `create trims values and converts blank optional fields to null`() {
        val wip = CreateWipRequest(
            type = " commission ", species = " cat ", artistName = "Artist",
            otherCharacters = " ", quality = " ", orderedDate = " ", payedDate = " ", deadlineDate = " ", note = " ",
            currency = Currency.USD, amount = 12.0, isNsfw = true,
        ).toEntity(TestFixtures.artist())

        assertEquals(0L, wip.id)
        assertEquals("commission", wip.type)
        assertEquals("cat", wip.species)
        assertEquals(Currency.USD, wip.price.currency)
        assertEquals(12.0, wip.price.amount)
        assertEquals(true, wip.isNsfw)
        assertNull(wip.otherCharacters)
        assertNull(wip.quality)
        assertNull(wip.orderedDate)
        assertNull(wip.payedDate)
        assertNull(wip.deadlineDate)
        assertNull(wip.note)
    }

    @Test
    fun `update clears optional values and preserves identity and artist`() {
        val wip = TestFixtures.wip()
        val artist = wip.artist

        UpdateWipRequest(type = " sketch ", species = " fox ", deadlineDate = " ", note = " ").applyTo(wip)

        assertEquals(1L, wip.id)
        assertEquals(artist, wip.artist)
        assertEquals("sketch", wip.type)
        assertEquals("fox", wip.species)
        assertNull(wip.otherCharacters)
        assertNull(wip.quality)
        assertNull(wip.orderedDate)
        assertNull(wip.payedDate)
        assertNull(wip.deadlineDate)
        assertNull(wip.note)
    }

    @Test
    fun `delivery inherits all omitted fields`() {
        val wip = TestFixtures.wip(isNsfw = true)
        val request = DeliverArtRequest(deliveredDate = " 2024-02-01 ", fileName = " art ").toCreateRequest(wip)

        assertEquals(wip.type, request.type)
        assertEquals(wip.species, request.species)
        assertEquals(wip.otherCharacters, request.otherCharacters)
        assertEquals(wip.quality, request.quality)
        assertEquals(wip.orderedDate, request.orderedDate)
        assertEquals(wip.payedDate, request.payedDate)
        assertEquals(wip.price.currency, request.currency)
        assertEquals(wip.price.amount, request.amount)
        assertEquals(wip.note, request.note)
        assertEquals(wip.artist.name, request.artistName)
        assertEquals(true, request.isNsfw)
        assertEquals("2024-02-01", request.deliveredDate)
        assertEquals("art", request.fileName)
        assertNull(request.link)
    }

    @Test
    fun `delivery allows explicit overrides including false and zero`() {
        val request = DeliverArtRequest(
            type = " sketch ", species = " fox ", otherCharacters = "Dog", quality = "Low",
            deliveredDate = "2024-02-01", fileName = "art", currency = Currency.EUR,
            amount = 0.0, note = "New", isNsfw = false, link = " https://example.com ",
        ).toCreateRequest(TestFixtures.wip(isNsfw = true))

        assertEquals("sketch", request.type)
        assertEquals("fox", request.species)
        assertEquals("Dog", request.otherCharacters)
        assertEquals("Low", request.quality)
        assertEquals(Currency.EUR, request.currency)
        assertEquals(0.0, request.amount)
        assertEquals("New", request.note)
        assertEquals(false, request.isNsfw)
        assertEquals("https://example.com", request.link)
    }
}
