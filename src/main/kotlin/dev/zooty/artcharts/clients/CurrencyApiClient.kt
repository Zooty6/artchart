package dev.zooty.artcharts.clients

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.ResponseSpec
import org.springframework.web.client.body

@Component
class CurrencyApiClient(@param:Value("\${artcharts.currencyapi.apiKey}") private val apiKey: String) {
    private val mapper = ObjectMapper()
    private val cache = HashMap<Pair<String, String>, Double>()
    private val client: RestClient = RestClient.builder()
        .requestFactory(HttpComponentsClientHttpRequestFactory())
        .baseUrl("https://currencyapi.com")
        .defaultHeader("apikey", apiKey)
        .build()

    fun getConvertRate(from: String, to: String): Double {
        val pair = Pair(from, to)
        if (!cache.containsKey(pair)) {
            cache[pair] = getFromRemote(from, to)
        }
        return cache[pair] ?: 0.0
    }

    private fun getFromRemote(from: String, to: String): Double {
        val response: ResponseSpec = client.get()
            .uri("/api/v3/latest?base_currency={base}&currencies={currency}", from, to)
            .retrieve()
        val body: String? = response.body<String>()
        val jsonNode = mapper.readTree(body)
        return jsonNode["data"][to]["value"].asDouble()
    }
}