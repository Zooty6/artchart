package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.dto.CreateWipRequest
import dev.zooty.artcharts.dto.DeliverArtRequest
import dev.zooty.artcharts.dto.UpdateWipRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.WipRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Wip
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class WipService(
    val wipRepository: WipRepository,
    val artistRepository: ArtistRepository,
    val artCreationService: ArtCreationService
) {
    fun getWips(): List<Wip> = wipRepository.findAll()
    
    fun getWip(id: Long): Wip = 
        wipRepository.findById(id).orElseThrow { ResourceNotFoundException("No wip with id $id found") }
    
    @Transactional
    fun create(request: CreateWipRequest): Wip {
        return wipRepository.save(
            request.toEntity(
                artistRepository.findByNameIgnoreCase(request.artistName.trim())
                    ?: throw ResourceNotFoundException("No artist with name ${request.artistName} found")
            )
        )
    }
    
    @Transactional
    fun update(id: Long, request: UpdateWipRequest) {
        request.applyTo(
            wipRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("No wip with id $id found") })
    }

    @Transactional
    fun deliverArt(wipId: Long, request: DeliverArtRequest, files: List<MultipartFile>): Art {
        val wip = wipRepository.findById(wipId).orElseThrow { ResourceNotFoundException("No wip with id $wipId found") }
        wipRepository.delete(wip)
        return artCreationService.create(request.toCreateRequest(wip), files)
    }

}