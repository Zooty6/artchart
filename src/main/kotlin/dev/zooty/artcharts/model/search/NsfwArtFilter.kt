package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

class NsfwArtFilter(private val nsfwString: String) : ArtFilter {
    override fun filter(art: Art): Boolean = art
        .isNsfw == nsfwString.toBoolean()
}