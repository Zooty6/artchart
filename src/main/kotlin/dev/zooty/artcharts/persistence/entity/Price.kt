package dev.zooty.artcharts.persistence.entity

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
    PLN,
    Gift,
    UNKNOWN
}
