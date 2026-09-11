package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

class TagArtFilter(private val tagName: String) : ArtFilter {
    override fun filter(art: Art): Boolean = art
        .tags
        .stream()
        .anyMatch { tag ->
            tag
                .name
                .lowercase()
                .contains(tagName.lowercase())
        }
}