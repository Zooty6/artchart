package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SiteQueryService(private val artRepository: ArtRepository) {
    fun allArts(): List<Art> = artRepository.findAll().sortedByDescending { it.deliveredDate }

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

    fun art(id: Long): Art = artRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Art with id $id not found") }

    fun yearOf(date: String): Int? = runCatching { LocalDate.parse(date).year }
        .getOrElse { date.take(4).toIntOrNull() }
}
