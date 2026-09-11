package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

class QualityArtFilter(private val quality: String) : ArtFilter {
    override fun filter(art: Art): Boolean = art
        .quality
        ?.lowercase()
        ?.contains(quality.lowercase()) ?: false
}