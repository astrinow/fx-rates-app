package com.fxrates.data.remote.dto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class TimeSeriesDtoTest : FunSpec({

    test("numeric JSON tokens retain all decimal digits without passing through double") {
        val response = """
            {
              "base": "USD",
              "start_date": "2026-10-06",
              "end_date": "2026-11-04",
              "rates": {
                "2026-11-04": {
                  "EUR": 0.123456789012345678901234567890123456789,
                  "JPY": 123456789012345678901234567890.123456789
                }
              }
            }
        """.trimIndent()

        val result = Json.decodeFromString<TimeSeriesDto>(response)
        val quotes = result.rates.getValue("2026-11-04")

        result.base shouldBe "USD"
        result.startDate shouldBe "2026-10-06"
        result.endDate shouldBe "2026-11-04"
        quotes.getValue("EUR").content shouldBe "0.123456789012345678901234567890123456789"
        quotes.getValue("JPY").content shouldBe "123456789012345678901234567890.123456789"
        quotes.getValue("EUR").isString shouldBe false
    }

    test("scientific notation and trailing decimal zeros retain their numeric tokens") {
        val response = """
            {
              "base": "USD",
              "start_date": "2026-11-04",
              "end_date": "2026-11-04",
              "rates": {"2026-11-04": {"EUR": 1.234567890123456789e-20, "JPY": 157.9000, "CHF": 0}}
            }
        """.trimIndent()

        val quotes = Json.decodeFromString<TimeSeriesDto>(response).rates.getValue("2026-11-04")

        quotes.getValue("EUR").content shouldBe "1.234567890123456789e-20"
        quotes.getValue("JPY").content shouldBe "157.9000"
        quotes.getValue("CHF").content shouldBe "0"
    }

    test("an empty rates object is a valid response") {
        val response = """
            {"base": "USD", "start_date": "2026-10-06", "end_date": "2026-11-04", "rates": {}}
        """.trimIndent()

        Json.decodeFromString<TimeSeriesDto>(response).rates shouldBe emptyMap()
    }

})
