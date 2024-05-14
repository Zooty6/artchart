package dev.zooty.artcharts.services

import dev.zooty.artcharts.clients.CurrencyApiClient
import org.springframework.stereotype.Service

@Service
class CurrencyService(
    private val currencyApiClient: CurrencyApiClient,
) {
    fun convertCurrency(amount: Int, from: String, to: String): Double {
        val convertRate = currencyApiClient.getConvertRate(from, to)
        return amount * convertRate
    }
}
