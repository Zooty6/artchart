package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.persistence.TagRepository
import org.springframework.stereotype.Service

@Service
class TagService(private val tagRepository: TagRepository) {
    fun findAllDistinctCategories(): List<String> = tagRepository.findDistinctCategories()
}
