package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtCreationService(
    private val artRepository: ArtRepository,
    private val artistRepository: ArtistRepository,
) {
    @Transactional
    fun create(request: CreateArtRequest) = artRepository.save(
        request.toEntity(
            artistRepository.findByNameIgnoreCase(request.artistName.trim())
                ?: throw ResourceNotFoundException("Artist ${request.artistName} not found")
        )
    )
}
