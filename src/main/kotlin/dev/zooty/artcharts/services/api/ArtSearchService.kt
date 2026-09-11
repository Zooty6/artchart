package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.model.search.ArtFilter
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtSearchService(private val artRepository: ArtRepository) {

    @Transactional(readOnly = true)
    fun searchArts(searchParams: String): List<Art> =
        searchParams
            .split(" ")
            .map(ArtFilter::createFilter)
            .fold(artRepository.findAllBy()) { arts, filter ->
                arts.filter(filter::filter)
            }.toList()
}
