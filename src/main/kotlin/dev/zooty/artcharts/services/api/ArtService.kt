package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.TagRepository
import dev.zooty.artcharts.persistence.entity.Tag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtService(val artRepository: ArtRepository, val tagRepository: TagRepository) {
    @Transactional
    fun addTag(id: Long, tag: TagDto) {
        artRepository.findById(id)
            .ifPresentOrElse(
                { art ->
                    art.tags.add(
                        tagRepository.findByName(tag.name)
                            .orElseGet { createTag(tag) }
                    )
                },
                { throw ResourceNotFoundException("Art with id $id not found") })
    }

    private fun createTag(tagDto: TagDto): Tag {
        return tagRepository.save(tagDto.toEntity())
    }
}
