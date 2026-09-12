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
}
