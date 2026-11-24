package dev.zooty.artcharts.mcp

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.dto.CreateArtistRequest
import dev.zooty.artcharts.dto.CreateWipRequest
import dev.zooty.artcharts.dto.DeliverArtRequest
import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.dto.UpdateArtRequest
import dev.zooty.artcharts.dto.UpdateArtistRequest
import dev.zooty.artcharts.dto.UpdateWipRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.TagRepository
import dev.zooty.artcharts.persistence.WipRepository
import dev.zooty.artcharts.services.api.ArtSearchService
import dev.zooty.artcharts.services.api.ArtService
import dev.zooty.artcharts.services.api.ArtistService
import dev.zooty.artcharts.services.api.TagService
import dev.zooty.artcharts.services.api.WipService
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations
import jakarta.validation.Valid
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import mu.KotlinLogging
import org.springframework.ai.util.json.schema.JsonSchemaGenerator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.ai.mcp.server.enabled", havingValue = "true")
internal class McpTools(
    private val validator: Validator,
    private val arts: ArtRepository,
    private val artists: ArtistRepository,
    private val tags: TagRepository,
    private val wips: WipRepository,
    private val artService: ArtService,
    private val artistService: ArtistService,
    private val wipService: WipService,
    private val tagService: TagService,
    private val artSearch: ArtSearchService,
    private val media: McpMedia,
    private val charts: McpCharts,
) {
    private val logger = KotlinLogging.logger {}
    private val json = jacksonObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
    private val mapType = object : TypeReference<Map<String, Any>>() {}

    @Bean
    fun artchartMcpTools(): List<SyncToolSpecification> = listOf(
        read<PageInput>("list_arts", "List artworks ordered by ascending ID. Use page starting at 0 and pageSize from 1 to 100. The result includes items, page, pageSize, and hasNext.") {
            queryPage(it) { page -> arts.findAll(page) }
        },
        read<IdInput>("get_art", "Get one artwork by its positive database ID. Returns an error if no artwork has that ID.") { art(it.id) },
        read<SearchInput>("search_arts", "Search artworks with space-separated field:value conditions. All conditions must match. Text values are case-insensitive partial matches. Fields: tag (tag name), species, artist, otherCharacters or other, type, quality, nsfw:true|false, deliveredDate or delivered, paidDate or paid, orderedDate or ordered. Date values accept YYYY or YYYY-MM-DD and support =, >, >=, <, <= before the date, for example deliveredDate:>=2024-01-01 artist:fox. Default page is 0, default pageSize is 25, maximum pageSize is 100. The result includes items, page, pageSize, and hasNext.") {
            val results = artSearch.searchArts(it.query).sortedBy { art -> art.id }
            val start = (it.page.toLong() * it.pageSize).coerceAtMost(results.size.toLong()).toInt()
            val end = minOf(start.toLong() + it.pageSize, results.size.toLong()).toInt()
            pageResult(results.subList(start, end), it.paging(), end < results.size)
        },
        read<PageInput>("list_artists", "List artists ordered by ascending ID. Use page starting at 0 and pageSize from 1 to 100. The result includes items, page, pageSize, and hasNext.") {
            queryPage(it) { page -> artists.findAll(page) }
        },
        read<IdInput>("get_artist", "Get one artist by its positive database ID. Returns an error if no artist has that ID.") { artist(it.id) },
        read<SearchInput>("search_artists", "Find artists whose name contains the query, case-insensitively. Supports pagination with page starting at 0 and pageSize from 1 to 100. The result includes items, page, pageSize, and hasNext.") {
            queryPage(it.paging()) { page -> artists.findByNameContainingIgnoreCase(it.query, page) }
        },
        read<SearchInput>("search_tags", "Find tags whose name contains the query, case-insensitively. Results are ordered by name. Supports pagination with page starting at 0 and pageSize from 1 to 100. The result includes items, page, pageSize, and hasNext.") {
            queryPage(it.paging(), "name") { page -> tags.findByNameContainingIgnoreCase(it.query, page) }
        },
        read<EmptyInput>("list_tag_categories", "List distinct tag categories currently used in the database.") { tagService.findAllDistinctCategories() },
        read<PageInput>("list_wips", "List work-in-progress records ordered by ascending ID. Use page starting at 0 and pageSize from 1 to 100. The result includes items, page, pageSize, and hasNext.") {
            queryPage(it) { page -> wips.findAll(page) }
        },
        read<IdInput>("get_wip", "Get one work-in-progress record by its positive database ID. Returns an error if no WIP has that ID.") { wipService.getWip(it.id) },
        write<ArtCreation>("create_art", "Create an artwork and upload 1 to 100 media files as Base64. Combined decoded media must not exceed 8 MiB. Supply request fields for artwork details and a matching fileName. Dates use YYYY-MM-DD. The artist must already exist.") {
            val request = it.request.copy(fileName = it.request.fileName.trim(), deliveredDate = it.request.deliveredDate.trim())
            media.validateNewLocation(request.fileName, request.deliveredDate)
            artService.create(request, media.multipartFiles(it.files, request.fileName))
        },
        write<ArtUpdate>("update_art", "Update an artwork by ID. Supply the complete request with type, species, deliveredDate, currency, amount, and isNsfw as well as any optional fields to retain. Changing delivery year or NSFW visibility requires stored media to be deleted first. Dates use YYYY-MM-DD.") {
            media.withMediaLock {
                val art = art(it.id)
                media.validateMove(art, it.request)
                artService.update(it.id, it.request)
                art(it.id)
            }
        },
        write<IdInput>("delete_art", "Delete an artwork record by positive ID. Stored media files are separate and must be deleted with delete_art_media before deleting the artwork.") {
            media.withMediaLock { artService.delete(it.id) }
            mapOf("id" to it.id)
        },
        write<TagInput>("add_art_tag", "Attach a tag to an artwork by ID. Creates the tag if its name and category do not already identify one. Provide both tag name and category.") {
            artService.addTag(it.id, TagDto(it.name, it.category))
            art(it.id)
        },
        write<RemoveTagInput>("remove_art_tag", "Remove a tag from an artwork by artwork ID and exact tag name.") {
            artService.removeTag(it.id, it.name)
            art(it.id)
        },
        write<CreateArtistRequest>("create_artist", "Create an artist. Name is required. Optional fields store profile links, PayPal email, and a note. Use the artist's existing name in artwork and WIP creation.") { artistService.create(it) },
        write<ArtistUpdate>("update_artist", "Update an artist by ID. Supply the complete request, including name and every profile, email, or note field you want to keep. Omitted optional fields are cleared.") {
            artistService.update(it.id, it.request)
            artist(it.id)
        },
        write<CreateWipRequest>("create_wip", "Create a work-in-progress record. Type, species, and an existing artistName are required. Dates use YYYY-MM-DD. Currency is one of USD, EUR, HUF, JPY, GBP, MXN, RUB, PLN, CAD, Gift, or UNKNOWN.") { wipService.create(it) },
        write<WipUpdate>("update_wip", "Update a WIP by ID. Supply the complete request with type, species, currency, amount, and isNsfw plus optional fields to retain. Omitted optional fields are cleared. Dates use YYYY-MM-DD.") {
            wipService.update(it.id, it.request)
            wipService.getWip(it.id)
        },
        write<Delivery>("deliver_wip", "Convert a WIP into a completed artwork. Supply its positive ID, delivery date (YYYY-MM-DD), and fileName. Artwork details default to the WIP values and can be overridden. Upload zero to 100 Base64 media files, with at most 8 MiB decoded total.") {
            media.validateNewLocation(it.request.fileName, it.request.deliveredDate)
            wipService.deliverArt(it.id, it.request, media.multipartFiles(it.files, it.request.fileName.trim()))
        },
        read<IdInput>("list_art_media", "List stored media files for an artwork by positive ID. Returns file names, media types, and absolute download URLs.") { media.list(it.id) },
        write<McpMedia.Deletion>("delete_art_media", "Permanently delete a stored media file. Specify the artwork ID and exact file name returned by list_art_media. This does not delete the artwork record.") { media.delete(it) },
        read<McpCharts.Dimensions>("get_artist_distribution_chart", "Get artwork counts by artist, along with the chart data and SVG URL. Width and height control SVG dimensions and default to 1800 by 900 pixels.", charts::artistDistribution),
        read<McpCharts.CurrencyDistribution>("get_currency_distribution_chart", "Get yearly art spending grouped by currency and converted to USD, along with chart data and SVG URL. filterList optionally excludes currencies by their codes, such as USD or EUR. Gift and UNKNOWN currencies are omitted. SVG dimensions default to 1800 by 900 pixels.", charts::currencyDistribution),
        read<McpCharts.Dimensions>("get_spending_over_time_chart", "Get total artwork spending per delivery year in USD, along with chart data and SVG URL. Gift and UNKNOWN currencies are omitted. Width and height control SVG dimensions and default to 1800 by 900 pixels.", charts::spendOverTime),
        read<McpCharts.Dimensions>("get_nsfw_ratio_chart", "Get the counts of NSFW and non-NSFW artworks, along with chart data and SVG URL. Width and height control SVG dimensions and default to 1800 by 900 pixels.", charts::nsfwRatio),
        read<McpCharts.SpeciesDistribution>("get_species_distribution_chart", "Get artwork counts grouped by character species, along with chart data and SVG URL. type selects PIE, TREEMAP, or BAR and defaults to PIE. SVG dimensions default to 1800 by 900 pixels.", charts::speciesDistribution),
        read<McpCharts.TagDistribution>("get_tag_distribution_chart", "Get artwork counts grouped by tag, along with chart data and SVG URL. type selects PIE, TREEMAP, or BAR and defaults to PIE. category optionally limits results to one exact tag category. SVG dimensions default to 1800 by 900 pixels.", charts::tagDistribution),
        read<McpCharts.CharacterGraph>("get_character_graph", "Get character co-occurrence data and graph SVG URL. layout selects CIRCLE, ORGANIC, FAST_ORGANIC, or LIST and defaults to ORGANIC. selfIncluded controls whether the configured self character is included and defaults to false. SVG dimensions default to 1800 by 900 pixels.", charts::characterGraph),
        read<McpCharts.Delivery>("get_delivery_time_chart", "Get artwork delivery-time data and chart SVG URL. year optionally filters the ordered and delivered date pairs to one calendar year in YYYY format. If omitted, includes all years. Only counts art pieces with an orderDate, since that's when a delivery time makes sense. SVG dimensions default to 1800 by 900 pixels.", charts::delivery),
    )

    private fun art(id: Long) = arts.findById(id).orElseThrow { ResourceNotFoundException("Artwork not found") }

    private fun artist(id: Long) = artists.findById(id).orElseThrow { ResourceNotFoundException("Artist not found") }

    private fun pageResult(items: List<*>, input: PageInput, hasNext: Boolean) =
        mapOf("items" to items, "page" to input.page, "pageSize" to input.pageSize, "hasNext" to hasNext)

    private fun queryPage(input: PageInput, sort: String = "id", find: (PageRequest) -> Page<*>): Map<String, Any> {
        val request = input.pageable(sort)
        if (request.offset > Int.MAX_VALUE) return pageResult(emptyList<Any>(), input, false)
        val page = find(request)
        return pageResult(page.content, input, page.hasNext())
    }

    private inline fun <reified T : Any> read(name: String, description: String, noinline call: (T) -> Any) =
        tool(T::class.java, name, description, true, call)

    private inline fun <reified T : Any> write(name: String, description: String, noinline call: (T) -> Any) =
        tool(T::class.java, name, description, false, call)

    private fun <T : Any> tool(type: Class<T>, name: String, description: String, readOnly: Boolean, call: (T) -> Any): SyncToolSpecification {
        val schema = if (type == EmptyInput::class.java) mapOf("type" to "object", "additionalProperties" to false)
            else json.readValue(JsonSchemaGenerator.generateForType(type), mapType)
        val tool = Tool.builder(name, schema).description(description)
            .annotations(ToolAnnotations.builder().readOnlyHint(readOnly).destructiveHint(!readOnly)
                .openWorldHint(false).build()).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val input = json.convertValue(request.arguments().orEmpty(), type)
                val violations = validator.validate(input).toSet()
                require(violations.isEmpty()) {
                    violations.map { "${it.propertyPath} ${it.message}" }.sorted().joinToString("; ")
                }
                val output = mapOf("result" to call(input))
                val text = json.writeValueAsString(output)
                CallToolResult.builder().structuredContent(json.readValue(text, mapType))
                    .addTextContent(text).isError(false).build()
            } catch (exception: ResourceNotFoundException) {
                error(exception.message ?: "Record not found")
            } catch (exception: IllegalArgumentException) {
                val message = if (exception.cause is com.fasterxml.jackson.core.JacksonException) "Invalid tool arguments"
                    else exception.message ?: "Invalid tool arguments"
                error(message)
            } catch (exception: Exception) {
                logger.error(exception) { "MCP tool $name failed" }
                error("Artchart could not complete the operation")
            }
        }
    }

    private fun error(message: String) = CallToolResult.builder().addTextContent(message).isError(true).build()
}

private class EmptyInput

internal data class IdInput(@field:Positive val id: Long)

internal data class PageInput(
    @field:PositiveOrZero val page: Int = 0,
    @field:Min(1) @field:Max(100) val pageSize: Int = 25,
) {
    fun pageable(property: String): PageRequest = PageRequest.of(page, pageSize, Sort.by(property))
}

internal data class SearchInput(
    @field:NotBlank val query: String,
    @field:PositiveOrZero val page: Int = 0,
    @field:Min(1) @field:Max(100) val pageSize: Int = 25,
) {
    fun paging() = PageInput(page, pageSize)
}

internal data class ArtUpdate(@field:Positive val id: Long, @field:Valid val request: UpdateArtRequest)
internal data class ArtistUpdate(@field:Positive val id: Long, @field:Valid val request: UpdateArtistRequest)
internal data class WipUpdate(@field:Positive val id: Long, @field:Valid val request: UpdateWipRequest)
internal data class ArtCreation(
    @field:Valid val request: CreateArtRequest,
    @field:Size(min = 1, max = 100) @field:Valid val files: List<McpMedia.Upload>,
)
internal data class Delivery(
    @field:Positive val id: Long,
    @field:Valid val request: DeliverArtRequest,
    @field:Size(max = 100) @field:Valid val files: List<McpMedia.Upload> = emptyList(),
)
internal data class TagInput(
    @field:Positive val id: Long,
    @field:NotBlank val name: String,
    @field:NotBlank val category: String,
)
internal data class RemoveTagInput(
    @field:Positive val id: Long,
    @field:NotBlank val name: String,
)
