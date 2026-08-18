package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateArtRequest(
    @field:NotBlank val type: String = "",
    val otherCharacters: String? = null,
    val quality: String? = null,
    @field:NotBlank val species: String = "",
    val orderedDate: String? = null,
    val payedDate: String? = null,
    @field:NotBlank val deliveredDate: String = "",
    @field:NotBlank val fileName: String = "",
    @field:NotNull val currency: Currency? = Currency.UNKNOWN,
    val amount: Double = 0.0,
    val note: String? = null,
    @field:NotBlank val artistName: String = "",
    val isNsfw: Boolean = false,
    val link: String? = null,
) {
    fun toEntity(artist: Artist) = Art(
        0L, otherCharacters.nullIfBlank(), type.trim(), quality.nullIfBlank(), species.trim(),
        orderedDate.nullIfBlank(), payedDate.nullIfBlank(), deliveredDate.trim(), fileName.trim(),
        Price(currency ?: Currency.UNKNOWN, amount), note.nullIfBlank(), artist, isNsfw,
        link.nullIfBlank()
    )
}
