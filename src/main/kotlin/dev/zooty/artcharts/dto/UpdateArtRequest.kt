package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateArtRequest(
    @field:NotBlank val type: String = "",
    val otherCharacters: String? = null,
    val quality: String? = null,
    @field:NotBlank val species: String = "",
    val orderedDate: String? = null,
    val payedDate: String? = null,
    @field:NotBlank val deliveredDate: String = "",
    @field:NotNull var currency: Currency? = Currency.UNKNOWN,
    val amount: Double = 0.0,
    val link: String? = null,
) {
    companion object {
        fun from(art: Art) = UpdateArtRequest(
            type = art.type,
            otherCharacters = art.otherCharacters,
            quality = art.quality,
            species = art.species,
            orderedDate = art.orderedDate,
            payedDate = art.payedDate,
            deliveredDate = art.deliveredDate,
            currency = art.price.currency,
            amount = art.price.amount,
            link = art.link,
        )
    }

    fun applyTo(art: Art) {
        art.type = type.trim()
        art.otherCharacters = otherCharacters.nullIfBlank()
        art.quality = quality.nullIfBlank()
        art.species = species.trim()
        art.orderedDate = orderedDate.nullIfBlank()
        art.payedDate = payedDate.nullIfBlank()
        art.deliveredDate = deliveredDate.trim()
        art.price = Price(currency ?: Currency.UNKNOWN, amount)
        art.link = link.nullIfBlank()
    }
}
