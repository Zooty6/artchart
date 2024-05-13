package dev.zooty.artcharts.persistence.controllers.api

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.model.Art
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController()
class ArtController(val artRepository: ArtRepository) {
    @GetMapping("/api/art", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArts(): List<Art> {
        return artRepository.findAll()
    }

    @GetMapping("/api/art/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArts(@PathVariable id: Long): Art {
        return artRepository.getReferenceById(id)
    }
}