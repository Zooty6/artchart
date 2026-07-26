package dev.zooty.artcharts.services

import dev.zooty.artcharts.clients.CurrencyApiClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CurrencyServiceTest {

    @Mock
    lateinit var currencyApiClient: CurrencyApiClient

    @Test
    fun `convertCurrency multiplies by remote rate`() {
        `when`(currencyApiClient.getConvertRate("EUR", "USD")).thenReturn(1.2)
        val service = CurrencyService(currencyApiClient)

        assertEquals(12.0, service.convertCurrency(10.0, "EUR", "USD"))
    }
}
