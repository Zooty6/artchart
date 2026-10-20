package dev.zooty.artcharts.persistence.entity

import org.hibernate.type.descriptor.WrapperOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.stream.Stream

class PriceTypeTest {
    private val priceType = PriceType()

    @ParameterizedTest
    @MethodSource("pricesFromDatabase")
    fun `nullSafeGet parses supported values`(input: String?, expected: Price) {
        val resultSet = mock(ResultSet::class.java)
        `when`(resultSet.getString(1)).thenReturn(input)

        assertEquals(expected, priceType.nullSafeGet(resultSet, 1, null))
    }

    @ParameterizedTest
    @MethodSource("valuesForDatabase")
    fun `nullSafeSet formats every currency`(price: Price?, expected: String?, isNull: Boolean) {
        val statement = mock(PreparedStatement::class.java)

        priceType.nullSafeSet(statement, price, 2, null as WrapperOptions?)

        if (isNull) {
            verify(statement).setNull(2, Types.VARCHAR)
        } else {
            verify(statement).setString(2, expected)
        }
    }

    companion object {
        @JvmStatic
        fun pricesFromDatabase(): Stream<Arguments> = Stream.of(
            Arguments.of("$12.5", Price(Currency.USD, 12.5)),
            Arguments.of("€ 12.5", Price(Currency.EUR, 12.5)),
            Arguments.of("Ft 12.5", Price(Currency.HUF, 12.5)),
            Arguments.of("huf 12.5", Price(Currency.HUF, 12.5)),
            Arguments.of("JPY 12.5", Price(Currency.JPY, 12.5)),
            Arguments.of("GBP 12.5", Price(Currency.GBP, 12.5)),
            Arguments.of("MXN 12.5", Price(Currency.MXN, 12.5)),
            Arguments.of("RUB 12.5", Price(Currency.RUB, 12.5)),
            Arguments.of("PLN 12.5", Price(Currency.PLN, 12.5)),
            Arguments.of("cad 12.5", Price(Currency.CAD, 12.5)),
            Arguments.of("gift commission", Price(Currency.Gift, 0.0)),
            Arguments.of("reward", Price(Currency.Gift, 0.0)),
            Arguments.of("request", Price(Currency.Gift, 0.0)),
            Arguments.of("raffle", Price(Currency.Gift, 0.0)),
            Arguments.of("alt version", Price(Currency.Gift, 0.0)),
            Arguments.of(" ? ", Price(Currency.UNKNOWN, 0.0)),
            Arguments.of("unrecognized", Price(Currency.UNKNOWN, 0.0)),
            Arguments.of(null, Price(Currency.UNKNOWN, 0.0))
        )

        @JvmStatic
        fun valuesForDatabase(): Stream<Arguments> = Stream.of(
            Arguments.of(Price(Currency.USD, 12.5), "$12.5", false),
            Arguments.of(Price(Currency.EUR, 12.5), "€12.5", false),
            Arguments.of(Price(Currency.HUF, 12.5), "Ft 12.5", false),
            Arguments.of(Price(Currency.JPY, 12.5), "JPY 12.5", false),
            Arguments.of(Price(Currency.GBP, 12.5), "GBP 12.5", false),
            Arguments.of(Price(Currency.MXN, 12.5), "MXN 12.5", false),
            Arguments.of(Price(Currency.RUB, 12.5), "RUB 12.5", false),
            Arguments.of(Price(Currency.PLN, 12.5), "PLN 12.5", false),
            Arguments.of(Price(Currency.CAD, 12.5), "CAD 12.5", false),
            Arguments.of(Price(Currency.Gift, 12.5), "gift", false),
            Arguments.of(Price(Currency.UNKNOWN, 12.5), "?", false),
            Arguments.of(null, null, true)
        )
    }
}
