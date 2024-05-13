package dev.zooty.artcharts.persistence.model

import lombok.EqualsAndHashCode

@EqualsAndHashCode
class Price(val currency: Currency, val amount: Number)

enum class Currency {
    USD,
    EURO,
    HUF,
    Gift,
    UNKNOWN
}
