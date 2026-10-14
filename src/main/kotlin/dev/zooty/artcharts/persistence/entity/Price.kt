package dev.zooty.artcharts.persistence.entity

class Price(val currency: Currency, val amount: Double) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Price

        if (amount != other.amount) return false
        if (currency != other.currency) return false

        return true
    }

    override fun hashCode(): Int {
        var result = amount.hashCode()
        result = 31 * result + currency.hashCode()
        return result
    }
}

enum class Currency {
    USD,
    EUR,
    HUF,
    JPY,
    GBP,
    MXN,
    RUB,
    PLN,
    CAD,
    Gift,
    UNKNOWN
}
