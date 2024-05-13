package dev.zooty.artcharts.persistence.controllers

import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.model.Artist
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController()
class ArtistController(val artistRepository: ArtistRepository) {
    @GetMapping("/api/artist", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArtists(): MutableList<Artist> {
        return artistRepository.findAll()
    }

    @GetMapping("/api/artist/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArtist(@PathVariable id: Long): Artist {
        return artistRepository.getReferenceById(id)
    }
}