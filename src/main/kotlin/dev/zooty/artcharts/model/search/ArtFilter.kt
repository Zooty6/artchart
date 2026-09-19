package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art

sealed interface ArtFilter {
    fun filter(art: Art): Boolean

    companion object {
        fun createFilter(filterString: String): ArtFilter {
            val split = filterString.split(":")
            require(split.size == 2) { "Invalid filter string: $filterString" }
            return when (split[0]) {
                "tag" -> TagArtFilter(split[1])
                "artist" -> ArtistArtFilter(split[1])
                "otherCharacters" -> OtherCharacterArtFilter(split[1])
                "type" -> TypeArtFilter(split[1])
                "quality" -> QualityArtFilter(split[1])
                "nsfw" -> NsfwArtFilter(split[1])
                "deliveredDate" -> DeliveryDateArtFilter(split[1])
                "paidDate" -> PaidDateArtFilter(split[1])
                "orderedDate" -> OrderDateArtFilter(split[1])
                else -> throw IllegalArgumentException("Invalid filter string: $filterString")
            }
        }
        
        fun removeRelationSymbols(string: String): String {
            return string.replace("<", "")
                .replace(">", "")
                .replace("=", "")
        }
    }
}
