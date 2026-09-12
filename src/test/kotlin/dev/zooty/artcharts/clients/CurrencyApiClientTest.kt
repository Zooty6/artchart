package dev.zooty.artcharts.clients

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class CurrencyApiClientTest {

    @Test
    fun `second request for the same currency pair is served from cache`() {
        val client = CurrencyApiClient("test-key")
        val restClientBuilder = RestClient.builder().baseUrl("https://currencyapi.com")
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val restClient = restClientBuilder.build()
        ReflectionTestUtils.setField(client, "client", restClient)

        server.expect(
            requestTo("https://currencyapi.com/api/v3/latest?base_currency=EUR&currencies=USD")
        ).andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"data":{"USD":{"value":1.12}}}""",
                    MediaType.APPLICATION_JSON,
                )
            )

        assertEquals(1.12, client.getConvertRate("EUR", "USD"))
        assertEquals(1.12, client.getConvertRate("EUR", "USD"))

        server.verify()
    }

    @Test
    fun `expired cache entry triggers a new request`() {
        val client = CurrencyApiClient("test-key")
        val restClientBuilder = RestClient.builder().baseUrl("https://currencyapi.com")
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        ReflectionTestUtils.setField(client, "client", restClientBuilder.build())
        ReflectionTestUtils.setField(client, "cacheTtl", Duration.ofHours(3))

        val beforeExpiry = Instant.parse("2026-01-01T00:00:00Z")
        ReflectionTestUtils.setField(client, "clock", Clock.fixed(beforeExpiry, ZoneOffset.UTC))
        expectResponse(server, 1.12)
        expectResponse(server, 1.13)
        assertEquals(1.12, client.getConvertRate("EUR", "USD"))

        ReflectionTestUtils.setField(
            client,
            "clock",
            Clock.fixed(beforeExpiry.plus(Duration.ofHours(3)), ZoneOffset.UTC),
        )
        assertEquals(1.13, client.getConvertRate("EUR", "USD"))

        server.verify()
    }

    @Test
    fun `cleanup removes expired cache entries`() {
        val client = CurrencyApiClient("test-key")
        val restClientBuilder = RestClient.builder().baseUrl("https://currencyapi.com")
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        ReflectionTestUtils.setField(client, "client", restClientBuilder.build())
        val createdAt = Instant.parse("2026-01-01T00:00:00Z")
        ReflectionTestUtils.setField(client, "clock", Clock.fixed(createdAt, ZoneOffset.UTC))

        expectResponse(server, 1.12)
        expectResponse(server, 1.13)
        client.getConvertRate("EUR", "USD")

        ReflectionTestUtils.setField(
            client,
            "clock",
            Clock.fixed(createdAt.plus(Duration.ofHours(4)), ZoneOffset.UTC),
        )
        client.removeExpiredEntries()

        assertEquals(1.13, client.getConvertRate("EUR", "USD"))
        server.verify()
    }

    private fun expectResponse(server: MockRestServiceServer, rate: Double) {
        server.expect(
            requestTo("https://currencyapi.com/api/v3/latest?base_currency=EUR&currencies=USD")
        ).andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"data":{"USD":{"value":$rate}}}""",
                    MediaType.APPLICATION_JSON,
                )
            )
    }
}
