package dev.zooty.artcharts.clients

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.ResponseSpec
import org.springframework.web.client.body
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class CurrencyApiClient(@param:Value($$"${artcharts.currencyapi.apiKey}") private val apiKey: String) {
    private data class CacheEntry(val rate: Double, val expiresAt: Instant)

    private val mapper = ObjectMapper()
    private val cache = ConcurrentHashMap<Pair<String, String>, CacheEntry>()
    private val clock: Clock = Clock.systemUTC()

    @Value($$"${artcharts.currencyapi.cache-ttl:PT3H}")
    private var cacheTtl: Duration = Duration.ofHours(3)

    private val client: RestClient = RestClient.builder()
        .requestFactory(HttpComponentsClientHttpRequestFactory())
        .baseUrl("https://currencyapi.com")
        .defaultHeader("apikey", apiKey)
        .build()

    fun getConvertRate(from: String, to: String): Double {
        val pair = Pair(from, to)
        val now = Instant.now(clock)
        return getFromCache(pair, now)
            .map { it.rate }
            .orElseGet {
                val rate = getFromRemote(from, to)
                cache[pair] = CacheEntry(rate, now.plus(cacheTtl))
                rate
            }
    }

    private fun getFromCache(fromToPair: Pair<String, String>, now: Instant): Optional<CacheEntry> {
        return Optional.ofNullable(cache[fromToPair])
            .flatMap {
                if (it.expiresAt.isAfter(now)) Optional.of(it) else {
                    cache.remove(fromToPair)
                    Optional.empty()
                }
            }
    }

    private fun getFromRemote(from: String, to: String): Double {
        val response: ResponseSpec = client.get()
            .uri("/api/v3/latest?base_currency={base}&currencies={currency}", from, to)
            .retrieve()
        val body: String? = response.body<String>()
        val jsonNode = mapper.readTree(body)
        return jsonNode["data"][to]["value"].asDouble()
    }

    @Scheduled(fixedRate = 60 * 60 * 1000L)
    internal fun removeExpiredEntries() {
        val now = Instant.now(clock)
        cache.entries.removeIf { it.value.expiresAt <= now }
    }
}
