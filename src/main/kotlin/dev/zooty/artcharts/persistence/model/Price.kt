package dev.zooty.artcharts.persistence.model

import lombok.EqualsAndHashCode

@EqualsAndHashCode
class Price(val currency: Currency, val amount: Int)

enum class Currency {
    USD,
    EUR,
    HUF,
    Gift,
    UNKNOWN
}
