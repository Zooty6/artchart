package dev.zooty.artcharts.persistence.model

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class PriceType : UserType<Price> {
    override fun equals(x: Price?, y: Price?): Boolean {
        return x?.equals(y) == true
    }

    override fun hashCode(x: Price?): Int {
        return x.hashCode()
    }

    override fun getSqlType(): Int {
        return Types.VARCHAR
    }

    override fun returnedClass(): Class<Price> {
        return Price::class.java
    }

    override fun nullSafeGet(
        rs: ResultSet?,
        position: Int,
        session: SharedSessionContractImplementor?,
        owner: Any?
    ): Price {
        val priceString = rs?.getString(position)
        return when {
            priceString?.startsWith("$") == true -> Price(Currency.USD, extractValue(priceString, "$"))
            priceString?.startsWith("€") == true -> Price(Currency.EUR, extractValue(priceString, "€"))
            priceString?.startsWith("Ft", true) == true -> Price(
                Currency.HUF,
                extractValue(priceString, "Ft")
            )

            priceString?.startsWith("JPY", true) == true -> Price(
                Currency.JPY,
                extractValue(priceString, "JPY")
            )

            priceString?.startsWith("GBP", true) == true -> Price(
                Currency.GBP,
                extractValue(priceString, "GBP")
            )

            priceString?.trim()?.lowercase() == "gift" -> Price(Currency.Gift, 0)
            else -> Price(Currency.UNKNOWN, 0)
        }
    }

    private fun extractValue(priceString: String, currencyString: String): Int {
        return priceString.split(currencyString)[1].trim().toInt()
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun assemble(cached: Serializable?, owner: Any?): Price {
        return Price(Currency.UNKNOWN, 0)
    }

    override fun disassemble(value: Price?): Serializable {
        return value.toString()
    }

    override fun deepCopy(value: Price?): Price {
        return if (value == null)
            Price(Currency.UNKNOWN, 0)
        else
            Price(value.currency, value.amount)
    }

    override fun nullSafeSet(
        st: PreparedStatement?,
        value: Price?,
        index: Int,
        session: SharedSessionContractImplementor?
    ) {
        when {
            value == null -> st?.setNull(index, Types.VARCHAR)
            else -> {
                st?.setString(
                    index, when (value.currency) {
                        Currency.USD -> "\$${value.amount}"
                        Currency.EUR -> "€${value.amount}"
                        Currency.HUF -> "Ft ${value.amount}"
                        Currency.JPY -> "JPZ ${value.amount}"
                        Currency.GBP -> "GBP ${value.amount}"
                        Currency.Gift -> "gift"
                        Currency.UNKNOWN -> "?"
                    }
                )
            }
        }
    }
}