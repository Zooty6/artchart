package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art
import java.util.function.Function

sealed interface ArtFilter {
    fun filter(art: Art): Boolean

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

    class ArtistArtFilter(private val artistName: String) : ArtFilter {
        override fun filter(art: Art): Boolean = art
            .artist
            .name
            .lowercase()
            .contains(artistName.lowercase())
    }
    
    class OtherCharacterArtFilter(private val otherCharacter: String) : ArtFilter {
        override fun filter(art: Art): Boolean = art
            .otherCharacters
            ?.lowercase()
            ?.contains(otherCharacter.lowercase()) ?: false
    }
   
   class TypeArtFilter(private val typeName: String) : ArtFilter {
       override fun filter(art: Art): Boolean = art
           .type
           .lowercase()
           .contains(typeName.lowercase())
   }

    class QualityArtFilter(private val quality: String) : ArtFilter {
        override fun filter(art: Art): Boolean = art
            .quality
            ?.lowercase()
            ?.contains(quality.lowercase()) ?: false
    }
    
    class NsfwArtFilter(private val nsfwString: String) : ArtFilter {
        override fun filter(art: Art): Boolean = art
            .isNsfw == nsfwString.toBoolean()
    }

    class DeliveryDateArtFilter : RelationFilter, ArtFilter {
        private val dateString: String
        
        constructor(filterString: String) : super(filterString) {
            dateString = removeRelationSymbols(filterString)
        }

        override fun filter(art: Art): Boolean {
            return relationArtFilter(dateString, Function(Art::deliveredDate),::dateCompare, art)
        }
    }

    class PaidDateArtFilter : RelationFilter, ArtFilter {
        private val dateString: String
        
        constructor(filterString: String) : super(filterString) {
            dateString = removeRelationSymbols(filterString)
        }

        override fun filter(art: Art): Boolean {
            return relationArtFilter(dateString, Function(Art::payedDate),::dateCompare, art)
        }
    }
    
    class OrderDateArtFilter : RelationFilter, ArtFilter {
        private val dateString: String
        
        constructor(filterString: String) : super(filterString) {
            dateString = removeRelationSymbols(filterString)
        }
        
        override fun filter(art: Art): Boolean {
            return relationArtFilter(dateString, Function(Art::orderedDate), ::dateCompare, art)
        }
    } 

    companion object Factory {
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