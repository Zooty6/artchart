package dev.zooty.artcharts.services

import dev.zooty.artcharts.clients.CurrencyApiClient
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import org.jfree.data.category.CategoryDataset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SpendingServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository
    @Mock
    lateinit var currencyApiClient: CurrencyApiClient
    private val svgService = SvgConverterService()

    @Test
    fun `spendOverTime sums yearly spend with currency conversion`() {
        `when`(artRepository.findAll()).thenReturn(
            listOf(
                art(deliveredDate = "2024-01-15", price = Price(Currency.USD, 100.0)),
                art(deliveredDate = "2024-05-20", price = Price(Currency.EUR, 50.0))
            )
        )
        `when`(currencyApiClient.getConvertRate("EUR", "USD")).thenReturn(2.0)
        val service = SpendingService(svgService, artRepository, CurrencyService(currencyApiClient))

        val method = service.javaClass.getDeclaredMethod("createYearlySpendDataset")
        method.isAccessible = true
        val dataset = method.invoke(service) as CategoryDataset
        assertEquals(1, dataset.rowCount)
        assertEquals(1, dataset.columnCount)
        assertEquals(200.0, dataset.getValue(0, 0).toDouble())
    }

    @Test
    fun `currencyDistribution groups by currency and respects filter list`() {
        `when`(artRepository.findAll()).thenReturn(
            listOf(
                art(deliveredDate = "2024-01-15", price = Price(Currency.USD, 100.0)),
                art(deliveredDate = "2024-01-18", price = Price(Currency.EUR, 50.0))
            )
        )
        `when`(currencyApiClient.getConvertRate("USD", "USD")).thenReturn(1.0)
        `when`(currencyApiClient.getConvertRate("EUR", "USD")).thenReturn(2.0)
        val service = SpendingService(svgService, artRepository, CurrencyService(currencyApiClient))

        val method = service.javaClass.getDeclaredMethod("createCurrencyDistributionDataset", List::class.java)
        method.isAccessible = true
        val dataset = method.invoke(service, emptyList<String>()) as CategoryDataset
        assertEquals(2, dataset.rowCount)
        assertEquals(1, dataset.columnCount)
        assertEquals(100.0, dataset.getValue(0, 0).toDouble())
        assertEquals(100.0, dataset.getValue(1, 0).toDouble())
    }

    private fun art(
        deliveredDate: String,
        price: Price,
    ) = Art(
        id = 1,
        otherCharacters = null,
        type = "commission",
        quality = null,
        species = "cat",
        orderedDate = null,
        payedDate = null,
        deliveredDate = deliveredDate,
        fileName = "art.png",
        price = price,
        note = null,
        artist = Artist(1, "Artist", null, null, null, null, null, null, null, null, null, null, null),
        isNsfw = false,
        link = "https://example.com",
    )
}
