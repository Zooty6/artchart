package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

class TypeArtFilter(private val typeName: String) : ArtFilter {
   override fun filter(art: Art): Boolean = art
       .type
       .lowercase()
       .contains(typeName.lowercase())
}