package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art
import java.util.function.Function

class OrderDateArtFilter : RelationFilter, ArtFilter {
    private val dateString: String
    
    constructor(filterString: String) : super(filterString) {
        dateString = ArtFilter.removeRelationSymbols(filterString)
    }
    
    override fun filter(art: Art): Boolean {
        return relationArtFilter(dateString, Function(Art::orderedDate), ::dateCompare, art)
    }
}