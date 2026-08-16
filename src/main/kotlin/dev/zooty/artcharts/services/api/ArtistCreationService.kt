package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.dto.CreateArtistRequest
import dev.zooty.artcharts.persistence.ArtistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtistCreationService(private val artistRepository: ArtistRepository) {
    @Transactional
    fun create(request: CreateArtistRequest) = artistRepository.save(request.toEntity())
}
