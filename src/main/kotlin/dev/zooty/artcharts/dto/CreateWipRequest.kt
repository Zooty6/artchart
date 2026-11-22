package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import dev.zooty.artcharts.persistence.entity.Wip
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class CreateWipRequest (
    @field:NotBlank val type: String = "",
    val otherCharacters: String? = null,
    val quality: String? = null,
    @field:NotBlank val species: String = "",
    val orderedDate: String? = null,
    val payedDate: String? = null,
    @field:NotNull val currency: Currency = Currency.UNKNOWN,
    val amount: Double = 0.0,
    val note: String? = null,
    @field:NotBlank val artistName: String = "",
    val isNsfw: Boolean = false,
) {
    fun toEntity(artist: Artist): Wip = Wip(
        0L, otherCharacters.nullIfBlank(), type.trim(), quality.nullIfBlank(), species.trim(),
        orderedDate.nullIfBlank(), payedDate.nullIfBlank(), Price(currency, amount), 
        note.nullIfBlank(), artist, isNsfw,
    )
}
