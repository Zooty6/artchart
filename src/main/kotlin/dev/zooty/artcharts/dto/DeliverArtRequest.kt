package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Wip
import jakarta.validation.constraints.NotBlank

class DeliverArtRequest(
    val type: String? = null,
    val otherCharacters: String? = null,
    val quality: String? = null,
    @field:NotBlank val species: String? = null,
    @field:NotBlank val deliveredDate: String = "",
    @field:NotBlank val fileName: String = "",
    val currency: Currency? = null,
    val amount: Double? = null,
    val note: String? = null,
    val isNsfw: Boolean = false,
    val link: String? = null,
) {
    fun toCreateRequest(wip: Wip): CreateArtRequest = CreateArtRequest(
        type?.trim() ?: wip.type, otherCharacters ?: wip.otherCharacters, quality ?: wip.quality,
        species?.trim() ?: wip.species, wip.orderedDate, wip.payedDate, deliveredDate.trim(), fileName.trim(),
        currency ?: wip.price.currency, amount ?: wip.price.amount, note ?: wip.note,
        wip.artist.name, isNsfw, link.nullIfBlank() 
    )
}
