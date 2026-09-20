package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.services.site.SiteQueryService
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.Optional

private val logger = KotlinLogging.logger {}

@Service
class ArtCreationService(
    private val artRepository: ArtRepository,
    private val artistRepository: ArtistRepository,
    private val mediaFileService: MediaFileService
) {
    @Transactional
    @CacheEvict(cacheNames = ["art-years"], allEntries = true)
    fun create(request: CreateArtRequest, file: Optional<MultipartFile>): Art {
        file.ifPresent { trySaveFile(it, request) }
        return artRepository.save(
            request.toEntity(
                artistRepository.findByNameIgnoreCase(request.artistName.trim())
                    ?: throw ResourceNotFoundException("Artist ${request.artistName} not found")
            )
        );
    }
    
    private fun trySaveFile(file: MultipartFile, request: CreateArtRequest) {
        try {
            mediaFileService.saveFile(file, request.fileName, request.deliveredDate, request.isNsfw)
        } catch (exception: Exception) {
            logger.error("Couldn't save file ${request.fileName}", exception)
        }
    }
}
