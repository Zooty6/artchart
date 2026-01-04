package dev.zooty.artcharts.jobs

import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.services.api.ArtService
import jakarta.transaction.Transactional
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class SpeciesToTagJob(
    private val artRepository: ArtRepository,
    private val artService: ArtService
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        val arts = artRepository.findAll()
        saveTags(arts)
        editSpeciesInArt(arts)
    }

    private fun editSpeciesInArt(arts: List<Art>) {
        arts.forEach {
            it.species = it.species.split(",")[0].trim()
            artRepository.save(it)
        }
    }

    private fun saveTags(arts: List<Art>) {
        arts.stream()
            .map {
                it.id to it.species.split(",")
                    .stream()
                    .map(String::trim)
                    .map { tag -> TagDto(tag, "species") }
            }
            .forEach { it.second.forEach { tag -> artService.addTag(it.first, tag) } }
    }
}