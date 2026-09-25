package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.dto.CreateArtistRequest
import dev.zooty.artcharts.dto.UpdateArtistRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtistService(private val artistRepository: ArtistRepository) {
    @Transactional
    fun create(request: CreateArtistRequest) = artistRepository.save(request.toEntity())

    @Transactional
    fun update(id: Long, request: UpdateArtistRequest) {
        val artist = artistRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Artist with id $id not found") }
        request.applyTo(artist)
    }
}
