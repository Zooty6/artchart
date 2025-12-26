package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.model.Art
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
class ArtController(val artRepository: ArtRepository) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    
    @GetMapping("/api/art", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArts(): List<Art> {
        return artRepository.findAll()
    }

    @GetMapping("/api/art/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArts(@PathVariable id: Long): Art {
        val referenceById = artRepository.getReferenceById(id)
        log.info("Retrieved art: $referenceById")
        return referenceById;
    }
}