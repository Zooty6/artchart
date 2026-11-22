package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import dev.zooty.artcharts.persistence.entity.Wip
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class UpdateWipRequest (
    @field:NotBlank val type: String = "",
    val otherCharacters: String? = null,
    val quality: String? = null,
    @field:NotBlank val species: String = "",
    val orderedDate: String? = null,
    val payedDate: String? = null,
    @field:NotNull val currency: Currency = Currency.UNKNOWN,
    val amount: Double = 0.0,
    val note: String? = null,
    val isNsfw: Boolean = false, 
) {
    fun applyTo(wip: Wip) {
        wip.type = type.trim()
        wip.otherCharacters = otherCharacters.nullIfBlank()
        wip.quality = quality.nullIfBlank()
        wip.species = species.trim()
        wip.orderedDate = orderedDate.nullIfBlank()
        wip.payedDate = payedDate.nullIfBlank()
        wip.price = Price(currency, amount)
        wip.note = note
        wip.isNsfw = isNsfw
    }

}
