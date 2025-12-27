package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.services.api.ArtService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import lombok.extern.slf4j.Slf4j
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
class ArtController(val artRepository: ArtRepository, val artService: ArtService) {
    @GetMapping("/api/art", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArts(): List<Art> {
        return artRepository.findAll()
    }

    @GetMapping("/api/art/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArts(@PathVariable id: Long): Art {
        return artRepository.getReferenceById(id)
    }

    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Tag added successfully"),
        ApiResponse(responseCode = "404", description = "Art or tag not found")
    )
    @PostMapping("/api/art/{id}/tag", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addTag(@PathVariable id: Long, @RequestBody tag: TagDto): ResponseEntity<Void> { // NOSONAR(kotlin:S6508) swagger needs Void to show no response
        artService.addTag(id, tag)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(code = org.springframework.http.HttpStatus.NOT_FOUND)
    fun notFoundHandler() {
        // do nothing, just return 404
    }
}