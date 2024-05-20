package dev.zooty.artcharts.persistence.model

import lombok.EqualsAndHashCode

@EqualsAndHashCode
class Price(val currency: Currency, val amount: Double)

enum class Currency {
    USD,
    EUR,
    HUF,
    JPY,
    GBP,
    MXN,
    RUB,
    Gift,
    UNKNOWN
}
