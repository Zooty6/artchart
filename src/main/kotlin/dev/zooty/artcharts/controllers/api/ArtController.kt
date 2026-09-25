package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.services.api.ArtCreationService
import dev.zooty.artcharts.services.api.ArtSearchService
import dev.zooty.artcharts.services.api.ArtService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import lombok.extern.slf4j.Slf4j
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Optional

@Slf4j
@RestController
class ArtController(
    val artRepository: ArtRepository,
    val artService: ArtService,
    val artCreationService: ArtCreationService,
    val artSearchService: ArtSearchService
) {
    @GetMapping("/api/art", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArts(): List<Art> {
        return artRepository.findAll()
    }

    @PostMapping("/api/art", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createArt(@Valid @RequestBody request: CreateArtRequest): ResponseEntity<Art> =
        ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
            .body(artCreationService.create(request, Optional.empty()))

    @GetMapping("/api/art/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArt(@PathVariable id: Long): Art = artRepository.getReferenceById(id)

    @GetMapping("/api/art/search", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getFilteredArts(@RequestParam searchParams: String): List<Art> =
        artSearchService.searchArts(searchParams)

    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Tag added successfully"),
        ApiResponse(responseCode = "404", description = "Art or tag not found")
    )
    @PostMapping("/api/art/{id}/tag", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addTag(
        @PathVariable id: Long,
        @RequestBody tag: TagDto
    ): ResponseEntity<Void> { // NOSONAR(kotlin:S6508) swagger needs Void to show no response
        artService.addTag(id, tag)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/api/art/{id}/tag/{tagName}")
    fun removeTag(@PathVariable id: Long, @PathVariable tagName: String): ResponseEntity<Void> {
        artService.removeTag(id, tagName)
        return ResponseEntity.noContent().build()
    }
}
