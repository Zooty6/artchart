package dev.zooty.artcharts.mcp

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.zooty.artcharts.dto.*
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.TagRepository
import dev.zooty.artcharts.persistence.WipRepository
import dev.zooty.artcharts.services.api.*
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
        read<PageInput>("list_arts", "List artworks ordered by ID.") {
            queryPage(it) { page -> arts.findAll(page) }
        },
        read<IdInput>("get_art", "Get an artwork by ID.") { art(it.id) },
        read<SearchInput>("search_arts", "Search artworks with Artchart search syntax, e.g. species:wolf.") {
            val results = artSearch.searchArts(it.query).sortedBy { art -> art.id }
            val start = (it.page.toLong() * it.pageSize).coerceAtMost(results.size.toLong()).toInt()
            val end = minOf(start.toLong() + it.pageSize, results.size.toLong()).toInt()
            pageResult(results.subList(start, end), it.paging(), end < results.size)
        },
        read<PageInput>("list_artists", "List artists ordered by ID.") {
            queryPage(it) { page -> artists.findAll(page) }
        },
        read<IdInput>("get_artist", "Get an artist by ID.") { artist(it.id) },
        read<SearchInput>("search_artists", "Search artists by part of their name.") {
            queryPage(it.paging()) { page -> artists.findByNameContainingIgnoreCase(it.query, page) }
        },
        read<SearchInput>("search_tags", "Search tags by part of their name.") {
            queryPage(it.paging(), "name") { page -> tags.findByNameContainingIgnoreCase(it.query, page) }
        },
        read<EmptyInput>("list_tag_categories", "List tag categories.") { tagService.findAllDistinctCategories() },
        read<PageInput>("list_wips", "List work-in-progress records ordered by ID.") {
            queryPage(it) { page -> wips.findAll(page) }
        },
        read<IdInput>("get_wip", "Get a work-in-progress record by ID.") { wipService.getWip(it.id) },
        write<ArtCreation>("create_art", "Create an artwork and upload its Base64 media files (8 MiB total).") {
            val request = it.request.copy(fileName = it.request.fileName.trim(), deliveredDate = it.request.deliveredDate.trim())
            media.validateNewLocation(request.fileName, request.deliveredDate)
            artService.create(request, media.multipartFiles(it.files, request.fileName))
        },
        write<ArtUpdate>("update_art", "Replace editable artwork fields. Supply the complete update request.") {
            media.withMediaLock {
                val art = art(it.id)
                media.validateMove(art, it.request)
                artService.update(it.id, it.request)
                art(it.id)
            }
        },
        write<IdInput>("delete_art", "Delete an artwork record. Delete media separately before deleting the record.") {
            media.withMediaLock { artService.delete(it.id) }
            mapOf("id" to it.id)
        },
        write<TagInput>("add_art_tag", "Attach a tag to an artwork, creating the tag if needed.") {
            artService.addTag(it.id, TagDto(it.name, it.category))
            art(it.id)
        },
        write<RemoveTagInput>("remove_art_tag", "Remove a tag from an artwork.") {
            artService.removeTag(it.id, it.name)
            art(it.id)
        },
        write<CreateArtistRequest>("create_artist", "Create an artist.") { artistService.create(it) },
        write<ArtistUpdate>("update_artist", "Replace editable artist fields. Supply the complete update request.") {
            artistService.update(it.id, it.request)
            artist(it.id)
        },
        write<CreateWipRequest>("create_wip", "Create a work-in-progress record.") { wipService.create(it) },
        write<WipUpdate>("update_wip", "Replace editable WIP fields. Supply the complete update request.") {
            wipService.update(it.id, it.request)
            wipService.getWip(it.id)
        },
        write<Delivery>("deliver_wip", "Complete a WIP and optionally upload Base64 media files (8 MiB total).") {
            media.validateNewLocation(it.request.fileName, it.request.deliveredDate)
            wipService.deliverArt(it.id, it.request, media.multipartFiles(it.files, it.request.fileName.trim()))
        },
        read<IdInput>("list_art_media", "List stored files and download links for an artwork.") { media.list(it.id) },
        write<McpMedia.Deletion>("delete_art_media", "Permanently delete one media file by exact name.") { media.delete(it) },
        read<McpCharts.Query>("get_chart", "Get chart data and a link to the corresponding SVG.", charts::query),
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
