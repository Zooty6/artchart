package dev.zooty.artcharts.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.ByteArrayInputStream
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "spring.datasource.url=jdbc:hsqldb:mem:mcp-test",
    "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
    "spring.jpa.database-platform=org.hibernate.dialect.HSQLDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "artcharts.currencyapi.apiKey=test-key",
    "spring.ai.mcp.server.enabled=true",
    "artcharts.mcp.public-base-url=https://art.example",
])
class McpServerIT(@LocalServerPort private val port: Int) {
    private val json = jacksonObjectMapper()
    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

    @Test
    fun `client initializes and discovers typed tools`() {
        val server = rpc("initialize", mapOf(
            "protocolVersion" to "2025-11-25",
            "capabilities" to emptyMap<String, Any>(),
            "clientInfo" to mapOf("name" to "artchart-test", "version" to "1"),
        ))
        assertEquals("artchart", server.path("serverInfo").path("name").asText())
        val response = send(json.writeValueAsString(mapOf("jsonrpc" to "2.0", "method" to "notifications/initialized")))
        assertEquals(202, response.statusCode())
        val tools = rpc("tools/list").path("tools").associateBy { it.path("name").asText() }
        assertEquals(31, tools.size)
        assertTrue(tools.containsKey("get_species_distribution_chart"))
        assertFalse(tools.containsKey("get_chart"))
        assertFalse(tools.containsKey("replace_art_media"))
        assertTrue(tools.getValue("list_arts").path("annotations").path("readOnlyHint").asBoolean())
        assertTrue(tools.getValue("delete_art").path("annotations").path("destructiveHint").asBoolean())
        val uploadSchema = tools.getValue("create_art").path("inputSchema")
        assertEquals("array", uploadSchema.at("/properties/files/type").asText())
        assertFalse(tools.getValue("list_arts").at("/inputSchema/properties").has("pageable"))
        assertTrue(uploadSchema.at("/properties/request/properties").has("isNsfw"))
        assertTrue(call("list_arts", mapOf("pageSize" to 101)).path("isError").asBoolean())
        assertTrue(call("update_art", mapOf("id" to 1, "request" to emptyMap<String, Any>())).path("isError").asBoolean())
    }

    @Test
    fun `manages records media and chart data end to end`() {
        result("create_artist", mapOf("name" to "Test Artist"))
        assertEquals("Test Artist", result("find_artist_by_name", mapOf("name" to "test artist")).path("name").asText())
        assertEquals("Artist not found", result("find_artist_by_name", mapOf("name" to "missing artist")).asText())
        val wip = result("create_wip", mapOf(
            "type" to "Illustration", "species" to "Fox", "artistName" to "Test Artist", "orderedDate" to "2026-02-01",
        ))
        val wipId = wip.path("id").asLong()
        val artwork = result("deliver_wip", mapOf(
            "id" to wipId, "request" to mapOf("deliveredDate" to "2026-02-10", "fileName" to "fox.png"),
            "files" to uploads("fox.png"),
        ))
        val id = artwork.path("id").asLong()
        assertTrue(id > 0)
        assertTrue(call("get_wip", mapOf("id" to wipId)).path("isError").asBoolean())
        val page = result("search_arts", mapOf("query" to "species:Fox", "pageSize" to 1))
        assertEquals(id, page.at("/items/0/id").asLong())
        assertTrue(result("list_arts", mapOf("page" to Int.MAX_VALUE)).path("items").isEmpty)

        val chart = result("get_species_distribution_chart", mapOf("type" to "BAR"))
        assertEquals(1, chart.at("/data/Fox").asInt())
        assertEquals("https://art.example/chart/speciesDistribution?width=1800&height=900&type=BAR", chart.path("svgUrl").asText())
        val svg = get("/chart/speciesDistribution?width=1800&height=900&type=BAR")
        assertEquals(200, svg.statusCode())
        assertTrue(svg.body().contains("Fox"))

        val single = result("list_art_media", mapOf("id" to id))
        assertEquals("fox.png", single[0].path("fileName").asText())
        assertTrue(call("update_art", mapOf("id" to id, "request" to mapOf(
            "type" to "Illustration", "species" to "Fox", "deliveredDate" to "2027-02-10",
        ))).path("isError").asBoolean())

        val url = URI(single[0].path("url").asText())
        val down = get("${url.rawPath}?${url.rawQuery}")
        assertEquals("abc", down.body())
        assertTrue(down.headers().firstValue("Content-Disposition").orElse("")!!.startsWith("attachment"))
        assertTrue(result("delete_art_media", mapOf("id" to id, "fileName" to "fox.png")).isEmpty)
        assertEquals(404, get("${url.rawPath}?${url.rawQuery}").statusCode())
        assertEquals(id, result("delete_art", mapOf("id" to id)).path("id").asLong())
        assertTrue(call("get_art", mapOf("id" to id)).path("isError").asBoolean())
    }

    @Test
    fun `creates artwork and uploads media in one call`() {
        result("create_artist", mapOf("name" to "Media Artist"))
        val request = mapOf(
            "type" to "Illustration", "species" to "Otter", "artistName" to "Media Artist",
            "deliveredDate" to "2026-02-10", "fileName" to "otter",
        )
        assertTrue(call("create_art", mapOf("request" to request)).path("isError").asBoolean())
        assertTrue(call("create_art", mapOf("request" to request, "files" to uploads("../outside.png")))
            .path("isError").asBoolean())
        assertTrue(result("search_arts", mapOf("query" to "species:Otter")).path("items").isEmpty)

        val args = mapOf("request" to request, "files" to uploads("two.png", "one +.png"))
        val id = result("create_art", args).path("id").asLong()
        assertTrue(id > 0)
        assertEquals(1, result("search_arts", mapOf("query" to "species:Otter")).path("items").size())
        val stored = result("list_art_media", mapOf("id" to id))
        assertEquals(listOf("one +.png", "two.png"), stored.map { it.path("fileName").asText() })
        val url = URI(stored[0].path("url").asText())
        assertEquals("abc", get("${url.rawPath}?${url.rawQuery}").body())
    }

    @Test
    fun `searches and updates artists tags and work in progress records`() {
        val artist = result("create_artist", mapOf("name" to "Coverage Artist", "site" to "https://artist.example"))
        val artistId = artist.path("id").asLong()
        assertEquals(artistId, result("get_artist", mapOf("id" to artistId)).path("id").asLong())
        assertTrue(result("list_artists", mapOf("pageSize" to 100)).path("items").any {
            it.path("id").asLong() == artistId
        })
        assertEquals(artistId, result("search_artists", mapOf("query" to "coverage", "pageSize" to 1)).at("/items/0/id").asLong())
        assertEquals("Updated Coverage Artist", result("update_artist", mapOf(
            "id" to artistId,
            "request" to mapOf("name" to "Updated Coverage Artist", "note" to "updated by integration test"),
        )).path("name").asText())

        val artwork = result("create_art", mapOf(
            "request" to mapOf(
                "type" to "Illustration", "species" to "Lynx", "artistName" to "Updated Coverage Artist",
                "deliveredDate" to "2026-03-10", "fileName" to "lynx.png",
            ),
            "files" to uploads("lynx.png"),
        ))
        val artId = artwork.path("id").asLong()
        result("add_art_tag", mapOf("id" to artId, "name" to "integration", "category" to "test"))
        assertTrue(result("search_tags", mapOf("query" to "integr", "pageSize" to 1)).path("items").any {
            it.path("name").asText() == "integration"
        })
        assertTrue(result("list_tag_categories", emptyMap()).any { it.asText() == "test" })
        val withoutTag = result("remove_art_tag", mapOf("id" to artId, "name" to "integration"))
        assertFalse(withoutTag.path("tags").any { it.path("name").asText() == "integration" })

        val wip = result("create_wip", mapOf(
            "type" to "Sketch", "species" to "Lynx", "artistName" to "Updated Coverage Artist", "orderedDate" to "2026-04-01",
        ))
        val wipId = wip.path("id").asLong()
        assertEquals(wipId, result("list_wips", mapOf("pageSize" to 1)).at("/items/0/id").asLong())
        result("update_wip", mapOf(
            "id" to wipId,
            "request" to mapOf("type" to "Finished sketch", "species" to "Lynx", "currency" to "USD", "amount" to 25),
        ))
        assertEquals("Finished sketch", result("get_wip", mapOf("id" to wipId)).path("type").asText())
    }

    @Test
    fun `uses existing creation behavior when saving media fails`() {
        result("create_artist", mapOf("name" to "Upload Failure Artist"))
        Files.createDirectories(requireNotNull(mediaRoot).resolve("SFW/2026/blocked.png"))
        val request = mapOf(
            "type" to "Illustration", "species" to "Badger", "artistName" to "Upload Failure Artist",
            "deliveredDate" to "2026-02-10", "fileName" to "blocked.png",
        )
        val id = result("create_art", mapOf("request" to request, "files" to uploads("blocked.png"))).path("id").asLong()
        assertEquals(id, result("get_art", mapOf("id" to id)).path("id").asLong())
        assertTrue(result("list_art_media", mapOf("id" to id)).isEmpty)
    }

    @Test
    fun `checks origin and limits chunked and fixed length bodies`() {
        val request = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        assertEquals(403, send(request, "https://another.example").statusCode())
        assertEquals(200, send(request, "https://art.example").statusCode())
        val size = 32 * 1024 * 1024 + 1
        Socket("localhost", port).use { socket ->
            socket.soTimeout = 5_000
            val headers = listOf(
                "POST /mcp HTTP/1.1", "Host: localhost:$port", "Connection: close",
                "Content-Type: application/json", "Content-Length: $size", "", "",
            )
            socket.getOutputStream().write(headers.joinToString("\r\n").toByteArray())
            assertEquals("413", socket.getInputStream().bufferedReader().readLine().split(" ")[1])
        }
        val chunked = HttpRequest.BodyPublishers.ofInputStream { ByteArrayInputStream(ByteArray(size)) }
        assertEquals(413, client.send(post(chunked).build(), HttpResponse.BodyHandlers.discarding()).statusCode())
    }

    private fun uploads(vararg names: String) =
        names.map { mapOf("fileName" to it, "base64" to "YWJj") }

    private fun call(name: String, args: Map<String, Any>) = rpc("tools/call", mapOf("name" to name, "arguments" to args))

    private fun result(name: String, args: Map<String, Any>): JsonNode {
        val reply = call(name, args)
        assertFalse(reply.path("isError").asBoolean(), reply.toString())
        val structured = reply.path("structuredContent")
        assertEquals(structured, json.readTree(reply.at("/content/0/text").asText()))
        return structured.path("result")
    }

    private fun rpc(method: String, params: Map<String, Any> = emptyMap()): JsonNode {
        val response = send(json.writeValueAsString(mapOf("jsonrpc" to "2.0", "id" to 1, "method" to method, "params" to params)))
        assertEquals(200, response.statusCode(), response.body())
        val reply = json.readTree(response.body())
        assertTrue(reply.has("result"), reply.toString())
        return reply.path("result")
    }

    private fun send(body: String, origin: String? = null): HttpResponse<String> {
        val request = post(HttpRequest.BodyPublishers.ofString(body))
        origin?.let { request.header("Origin", it) }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun post(body: HttpRequest.BodyPublisher) = HttpRequest.newBuilder(URI("http://localhost:$port/mcp"))
        .timeout(Duration.ofSeconds(20))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .POST(body)

    private fun get(route: String): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(URI("http://localhost:$port$route")).build(), HttpResponse.BodyHandlers.ofString(),
    )

    companion object {
        @TempDir @JvmField
        var mediaRoot: Path? = null

        @JvmStatic
        @DynamicPropertySource
        @Suppress("unused")
        fun mediaProperties(registry: DynamicPropertyRegistry) {
            registry.add("artcharts.media-root") { requireNotNull(mediaRoot).toString() }
        }
    }
}
