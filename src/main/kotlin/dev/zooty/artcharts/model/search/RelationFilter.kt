package dev.zooty.artcharts.model.search

import dev.zooty.artcharts.persistence.entity.Art
import java.time.LocalDate
import java.util.function.Function

open class RelationFilter  {
    protected val relation: Relation
    
    constructor(filterString: String) {
        relation = when {
            filterString.startsWith(">=") -> Relation.GREATER_OR_EQUAL
            filterString.startsWith("=>") -> Relation.GREATER_OR_EQUAL
            filterString.startsWith("<=") -> Relation.LESS_OR_EQUAL
            filterString.startsWith("=<") -> Relation.LESS_OR_EQUAL
            filterString.startsWith("<") -> Relation.LESS_THAN
            filterString.startsWith(">") -> Relation.GREATER_THAN
            filterString.startsWith("=") -> Relation.EQUAL
            else -> Relation.EQUAL
        }
    }
    
    protected fun <T>relationArtFilter(control: T, artTestFieldGetter: Function<Art, T>, comparator: Comparator<T>, art: Art): Boolean {
        return artTestFieldGetter.apply(art) != null && when (relation) {
            Relation.GREATER_THAN -> comparator.compare(control, artTestFieldGetter.apply(art)) < 0
            Relation.LESS_THAN -> comparator.compare(control, artTestFieldGetter.apply(art)) > 0
            Relation.LESS_OR_EQUAL -> comparator.compare(control, artTestFieldGetter.apply(art)) >= 0
            Relation.GREATER_OR_EQUAL -> comparator.compare(control, artTestFieldGetter.apply(art)) <= 0
            Relation.EQUAL -> comparator.compare(control, artTestFieldGetter.apply(art)) == 0
        }
    }
    
    protected fun dateCompare(control: String, test: String): Int {
        return if (yearOnlyDate(control)) control.toInt().compareTo(test.substring(0, 4).toInt()) else LocalDate.parse(control).compareTo(LocalDate.parse(test))
    }
    
    protected fun yearOnlyDate(date: String): Boolean = date.length == 4
}