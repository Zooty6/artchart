package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.dto.CreateArtistRequest
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.services.api.ArtistCreationService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ArtistController(
    val artistRepository: ArtistRepository,
    val artistCreationService: ArtistCreationService,
) {
    @GetMapping("/api/artist", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArtists(): MutableList<Artist> {
        return artistRepository.findAll()
    }

    @GetMapping("/api/artist/search")
    fun searchArtists(@RequestParam query: String): List<Artist> =
        artistRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(query)

    @PostMapping("/api/artist", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createArtist(@Valid @RequestBody request: CreateArtistRequest): Artist =
        artistCreationService.create(request)

    @GetMapping("/api/artist/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArtist(@PathVariable id: Long): Artist {
        return artistRepository.getReferenceById(id)
    }
}
