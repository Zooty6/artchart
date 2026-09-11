package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

class ArtistArtFilter(private val artistName: String) : ArtFilter {
    override fun filter(art: Art): Boolean = art
        .artist
        .name
        .lowercase()
        .contains(artistName.lowercase())
}