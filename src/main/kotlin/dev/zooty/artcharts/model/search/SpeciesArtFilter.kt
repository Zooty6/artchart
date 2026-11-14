package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

class SpeciesArtFilter(private val species: String) : ArtFilter {
    override fun filter(art: Art): Boolean {
        return art.species
            .lowercase()
            .contains(species.lowercase())
    }
}