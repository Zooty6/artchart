package dev.zooty.artcharts.mcp

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.TagRepository
import dev.zooty.artcharts.persistence.WipRepository
import dev.zooty.artcharts.services.api.ArtSearchService
import dev.zooty.artcharts.services.api.ArtService
import dev.zooty.artcharts.services.api.ArtistService
import dev.zooty.artcharts.services.api.TagService
import dev.zooty.artcharts.services.api.WipService
import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpToolsTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator
    private val tools = McpTools(
        validator,
        Mockito.mock(ArtRepository::class.java),
        Mockito.mock(ArtistRepository::class.java),
        Mockito.mock(TagRepository::class.java),
        Mockito.mock(WipRepository::class.java),
        Mockito.mock(ArtService::class.java),
        Mockito.mock(ArtistService::class.java),
        Mockito.mock(WipService::class.java),
        Mockito.mock(TagService::class.java),
        Mockito.mock(ArtSearchService::class.java),
        Mockito.mock(McpMedia::class.java),
        Mockito.mock(McpCharts::class.java),
    ).artchartMcpTools().associateBy { it.tool().name() }

    @Test
    fun `registers all expected tools with descriptions and object schemas`() {
        val expected = setOf(
            "list_arts", "get_art", "search_arts", "list_artists", "get_artist", "find_artist_by_name", "search_artists", "search_tags",
            "list_tag_categories", "list_wips", "get_wip", "create_art", "update_art", "delete_art", "add_art_tag",
            "remove_art_tag", "create_artist", "update_artist", "create_wip", "update_wip", "deliver_wip",
            "list_art_media", "delete_art_media", "get_artist_distribution_chart", "get_currency_distribution_chart",
            "get_spending_over_time_chart", "get_nsfw_ratio_chart", "get_species_distribution_chart",
            "get_tag_distribution_chart", "get_character_graph", "get_delivery_time_chart",
        )

        assertEquals(expected, tools.keys)
        tools.values.forEach { specification ->
            assertTrue(specification.tool().description().isNotBlank())
            assertEquals("object", specification.tool().inputSchema()["type"])
        }
    }

    @Test
    fun `search description documents supported filters and date operators`() {
        val description = tools.getValue("search_arts").tool().description()

        listOf("tag", "species", "artist", "otherCharacters", "type", "quality", "nsfw", "deliveredDate", "paidDate", "orderedDate", "YYYY-MM-DD", ">=").forEach {
            assertTrue(description.contains(it), "search_arts description should include $it")
        }
    }

    @Test
    fun `marks query tools read only and mutation tools destructive`() {
        val annotations = tools.mapValues { it.value.tool().annotations() }

        assertTrue(annotations.getValue("search_arts").readOnlyHint())
        assertFalse(annotations.getValue("search_arts").destructiveHint())
        assertFalse(annotations.getValue("delete_art").readOnlyHint())
        assertTrue(annotations.getValue("delete_art").destructiveHint())
    }
}
