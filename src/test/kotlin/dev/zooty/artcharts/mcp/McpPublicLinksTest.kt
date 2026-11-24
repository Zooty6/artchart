package dev.zooty.artcharts.mcp

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class McpPublicLinksTest {
    @Test
    fun `uses loopback HTTP for the origin and generated links`() {
        for (host in listOf("localhost", "127.0.0.1", "[::1]")) {
            val base = "http://$host:8080"
            val links = McpPublicLinks("$base/artcharts/")

            assertEquals(base, links.origin)
            assertEquals("$base/artcharts/chart/speciesDistribution", links.url("/chart/speciesDistribution"))
        }
    }

    @Test
    fun `rejects HTTP for a non-loopback host`() {
        assertFailsWith<IllegalArgumentException> { McpPublicLinks("http://art.example:8080") }
    }
}
