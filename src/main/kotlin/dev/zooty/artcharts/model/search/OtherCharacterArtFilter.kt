package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

class OtherCharacterArtFilter(private val otherCharacter: String) : ArtFilter {
    override fun filter(art: Art): Boolean = art
        .otherCharacters
        ?.lowercase()
        ?.contains(otherCharacter.lowercase()) ?: false
}