package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.services.api.ArtSearchService
import org.springframework.stereotype.Service
import org.springframework.cache.annotation.Cacheable
import java.time.LocalDate

@Service
class SiteQueryService(
    private val artRepository: ArtRepository,
    private val artSearchService: ArtSearchService,
) {
    fun allArts(): List<Art> = artRepository.findAll().sortedByDescending { it.deliveredDate }

    @Cacheable("art-years")
    fun years(): List<Int> = allArts()
        .mapNotNull { yearOf(it.deliveredDate) }
        .distinct()
        .sortedDescending()

    fun artsForYear(year: Int?, hideNsfw: Boolean = false): List<Art> {
        if (year == null) return emptyList()
        return allArts()
            .filter { yearOf(it.deliveredDate) == year }
            .filter { !hideNsfw || !it.isNsfw }
    }

    fun artsForSearch(searchParams: String, hideNsfw: Boolean = false): List<Art> = artSearchService
        .searchArts(searchParams)
        .filter { !hideNsfw || !it.isNsfw }
        .sortedByDescending { it.deliveredDate }

    fun art(id: Long): Art = artRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Art with id $id not found") }

    fun yearOf(date: String): Int? = runCatching { LocalDate.parse(date).year }
        .getOrElse { date.take(4).toIntOrNull() }
}
