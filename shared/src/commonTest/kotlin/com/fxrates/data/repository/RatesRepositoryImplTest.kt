package com.fxrates.data.repository

import com.fxrates.core.utils.Resource
import com.fxrates.core.utils.resourceFlow
import com.fxrates.data.remote.FrankfurterApi
import com.fxrates.data.remote.configureHttpClient
import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.RateHistoryPoint
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

class RatesRepositoryImplTest : FunSpec({

    test("latest rates use the original v1 time series contract and the last published business days") {
        runTest {
            RepositoryFixture(response = LatestRatesResponse).use { fixture ->
                val rates = fixture.repository.getLatestRates(base = "USD")

                fixture.engine.requestHistory.map { request -> request.url.encodedPath }.sorted() shouldBe listOf(
                    "/v1/2026-10-28..",
                    "/v1/currencies",
                )
                val request = fixture.engine.requestHistory.single { request ->
                    request.url.encodedPath != "/v1/currencies"
                }
                request.url.host shouldBe "api.frankfurter.dev"
                request.url.parameters["base"] shouldBe "USD"
                val rate = rates.single()
                rate.code shouldBe "EUR"
                rate.name shouldBe "Euro"
                rate.date shouldBe LocalDate(2026, 11, 3)
                rate.rate shouldBe "0.91".toBigDecimal()
                rate.previousRate shouldBe "0.90".toBigDecimal()
                fixture.clock.readCount shouldBe 1
            }
        }
    }

    context("latest rates use calendar windows across weekends and month and year boundaries") {
        val windows = mapOf(
            "2026-01-01T00:00:00Z" to "2025-12-25..",
            "2024-03-01T00:00:00Z" to "2024-02-23..",
            "2026-11-01T23:30:00Z" to "2026-10-25..",
        )
        windows.forEach { (instant, window) ->
            test("the latest rates window at $instant is $window") {
                runTest {
                    RepositoryFixture(instant = instant, response = EmptyHistoryResponse).use { fixture ->
                        fixture.repository.getLatestRates(base = "GBP").shouldBeEmpty()

                        val request = fixture.engine.requestHistory.single { request ->
                            request.url.encodedPath != "/v1/currencies"
                        }
                        request.url.encodedPath shouldBe "/v1/$window"
                        request.url.parameters["base"] shouldBe "GBP"
                        fixture.clock.readCount shouldBe 1
                    }
                }
            }
        }
    }

    test("latest rates recalculate the start date after UTC midnight") {
        runTest {
            RepositoryFixture(instant = "2026-11-04T23:59:59Z", response = LatestRatesResponse).use { fixture ->
                fixture.repository.getLatestRates(base = "USD")
                fixture.clock.instant = Instant.parse("2026-11-05T00:00:00Z")

                fixture.repository.getLatestRates(base = "USD")

                fixture.engine.requestHistory.filter { request -> request.url.encodedPath != "/v1/currencies" }
                    .map { request -> request.url.encodedPath } shouldBe listOf(
                    "/v1/2026-10-28..",
                    "/v1/2026-10-29..",
                )
                fixture.clock.readCount shouldBe 2
            }
        }
    }

    context("unavailable time series fall back to the v1 latest endpoint") {
        listOf(500, 503, 521, 522, 504).forEach { status ->
            test("HTTP $status returns latest quotes without previous rates") {
                runTest {
                    RepositoryFixture(status = HttpStatusCode.fromValue(status)).use { fixture ->
                        val rates = fixture.repository.getLatestRates(base = "USD")

                        rates.map(CurrencyRate::code) shouldBe listOf("EUR", "JPY")
                        rates.map(CurrencyRate::previousRate) shouldBe listOf(null, null)
                        rates.map(CurrencyRate::date) shouldBe List(2) { LocalDate(2026, 11, 3) }
                        rates.map(CurrencyRate::rate) shouldBe listOf("0.91".toBigDecimal(), "160".toBigDecimal())
                        rates.map(CurrencyRate::name) shouldBe listOf("Euro", "JPY")
                        val requests = fixture.engine.requestHistory.filter { request ->
                            request.url.encodedPath != "/v1/currencies"
                        }
                        requests.map { request -> request.url.encodedPath } shouldBe listOf(
                            "/v1/2026-10-28..",
                            "/v1/latest",
                        )
                        requests.forEach { request ->
                            request.url.host shouldBe "api.frankfurter.dev"
                            request.url.parameters["base"] shouldBe "USD"
                        }
                    }
                }
            }
        }

        val failures = mapOf(
            "network failure" to IOException("connection lost"),
            "request timeout" to HttpRequestTimeoutException("https://api.frankfurter.dev/v1/2026-10-28..", 1000),
        )
        failures.forEach { (name, failure) ->
            test("a $name returns latest quotes with the requested base and exact decimal precision") {
                runTest {
                    val decimal = "0.123456789012345678901234567890123456789"
                    val latestResponse = FallbackResponse.replace("0.91", decimal).replace("USD", "GBP")
                    RepositoryFixture(failure = failure, latestResponse = latestResponse).use { fixture ->
                        val rates = fixture.repository.getLatestRates(base = "GBP")

                        rates.first().rate shouldBe decimal.toBigDecimal()
                        rates.map(CurrencyRate::previousRate) shouldBe listOf(null, null)
                        fixture.engine.requestHistory.single { request -> request.url.encodedPath == "/v1/latest" }
                            .url.parameters["base"] shouldBe "GBP"
                    }
                }
            }
        }
    }

    test("a failed fallback remains an error instead of becoming empty data") {
        runTest {
            RepositoryFixture(status = HttpStatusCode.fromValue(522), latestStatus = HttpStatusCode.BadGateway)
                .use { fixture ->
                    val states = resourceFlow { fixture.repository.getLatestRates(base = "USD") }.toList()

                    states.first() shouldBe Resource.Loading
                    val error = states.last().shouldBeInstanceOf<Resource.Error>().throwable
                    val status = error.shouldBeInstanceOf<ServerResponseException>().response.status
                    status shouldBe HttpStatusCode.BadGateway
                }
        }
    }

    test("a successful fallback ends resourceFlow in data") {
        runTest {
            RepositoryFixture(status = HttpStatusCode.fromValue(521)).use { fixture ->
                val states = resourceFlow { fixture.repository.getLatestRates(base = "USD") }.toList()

                states.size shouldBe 2
                states.first() shouldBe Resource.Loading
                states.last().shouldBeInstanceOf<Resource.Data<*>>()
            }
        }
    }

    test("a bad time series request does not fall back") {
        runTest {
            RepositoryFixture(status = HttpStatusCode.BadRequest).use { fixture ->
                shouldThrow<ClientRequestException> { fixture.repository.getLatestRates(base = "USD") }

                fixture.engine.requestHistory.none { request -> request.url.encodedPath == "/v1/latest" } shouldBe true
            }
        }
    }

    test("cancelling a list load does not request fallback rates") {
        runTest {
            RepositoryFixture(failure = CancellationException("cancelled")).use { fixture ->
                shouldThrow<CancellationException> { fixture.repository.getLatestRates(base = "USD") }

                fixture.engine.requestHistory.none { request -> request.url.encodedPath == "/v1/latest" } shouldBe true
            }
        }
    }

    test("malformed time series data does not trigger a fallback") {
        runTest {
            RepositoryFixture(response = "{}").use { fixture ->
                val states = resourceFlow { fixture.repository.getLatestRates(base = "USD") }.toList()

                states.last().shouldBeInstanceOf<Resource.Error>()
                fixture.engine.requestHistory.none { request -> request.url.encodedPath == "/v1/latest" } shouldBe true
            }
        }
    }

    test("history requests the inclusive 30-day UTC window in one v1 call") {
        runTest {
            RepositoryFixture(instant = "2026-11-04T23:30:00Z").use { fixture ->
                val history = fixture.repository.getRateHistory(base = "USD", code = "EUR")

                val request = fixture.engine.requestHistory.single()
                request.url.host shouldBe "api.frankfurter.dev"
                request.url.encodedPath shouldBe "/v1/2026-10-06..2026-11-04"
                request.url.parameters["base"] shouldBe "USD"
                history.base shouldBe "USD"
                history.code shouldBe "EUR"
                history.startDate shouldBe LocalDate(2026, 10, 6)
                history.endDate shouldBe LocalDate(2026, 11, 4)
                history.points shouldBe listOf(
                    RateHistoryPoint(LocalDate(2026, 10, 6), "0.89".toBigDecimal()),
                    RateHistoryPoint(LocalDate(2026, 11, 4), "0.91".toBigDecimal()),
                )
                fixture.clock.readCount shouldBe 1
            }
        }
    }

    context("calendar windows across month and year boundaries") {
        val windows = mapOf(
            "2026-01-01T00:00:00Z" to "2025-12-03..2026-01-01",
            "2024-03-01T00:00:00Z" to "2024-02-01..2024-03-01",
            "2026-03-01T00:00:00Z" to "2026-01-31..2026-03-01",
        )
        windows.forEach { (instant, window) ->
            test("the window at $instant is $window") {
                runTest {
                    RepositoryFixture(instant = instant).use { fixture ->
                        fixture.repository.getRateHistory(base = "USD", code = "EUR")

                        fixture.engine.requestHistory.single().url.encodedPath shouldBe "/v1/$window"
                    }
                }
            }
        }
    }

    test("a new load after UTC midnight recalculates both boundaries") {
        runTest {
            RepositoryFixture(instant = "2026-11-04T23:59:59Z").use { fixture ->
                fixture.repository.getRateHistory(base = "USD", code = "EUR")
                fixture.clock.instant = Instant.parse("2026-11-05T00:00:00Z")

                val history = fixture.repository.getRateHistory(base = "USD", code = "EUR")

                fixture.engine.requestHistory.map { request -> request.url.encodedPath } shouldBe listOf(
                    "/v1/2026-10-06..2026-11-04",
                    "/v1/2026-10-07..2026-11-05",
                )
                history.startDate shouldBe LocalDate(2026, 10, 7)
                history.endDate shouldBe LocalDate(2026, 11, 5)
            }
        }
    }

    test("the requested base and currency are used without requesting currency names") {
        runTest {
            RepositoryFixture(response = HistoryResponse.replace("USD", "GBP")).use { fixture ->
                val history = fixture.repository.getRateHistory(base = "GBP", code = "JPY")

                fixture.engine.requestHistory.single().url.parameters["base"] shouldBe "GBP"
                history.base shouldBe "GBP"
                history.code shouldBe "JPY"
                history.points.map(RateHistoryPoint::rate) shouldBe listOf("160".toBigDecimal())
            }
        }
    }

    test("an empty response is successful data and retains the requested window") {
        runTest {
            RepositoryFixture(response = EmptyHistoryResponse).use { fixture ->
                val history = fixture.repository.getRateHistory(base = "USD", code = "EUR")

                history.points.shouldBeEmpty()
                history.startDate shouldBe LocalDate(2026, 10, 6)
                history.endDate shouldBe LocalDate(2026, 11, 4)
            }
        }
    }

    test("an absent currency returns an empty history") {
        runTest {
            RepositoryFixture().use { fixture ->
                fixture.repository.getRateHistory(base = "USD", code = "CHF").points.shouldBeEmpty()
            }
        }
    }

    test("JSON decimal precision is preserved through the HTTP client and repository") {
        runTest {
            val response = """
                {
                  "base": "USD", "start_date": "2026-10-06", "end_date": "2026-11-04",
                  "rates": {"2026-11-04": {"EUR": 0.123456789012345678901234567890123456789}}
                }
            """.trimIndent()
            RepositoryFixture(response = response).use { fixture ->
                val history = fixture.repository.getRateHistory(base = "USD", code = "EUR")

                history.points.single().rate shouldBe "0.123456789012345678901234567890123456789".toBigDecimal()
            }
        }
    }

    test("HTTP server failures are thrown instead of becoming empty histories") {
        runTest {
            RepositoryFixture(status = HttpStatusCode.ServiceUnavailable).use { fixture ->
                shouldThrow<ServerResponseException> {
                    fixture.repository.getRateHistory(base = "USD", code = "EUR")
                }
            }
        }
    }

    test("HTTP client failures are thrown instead of becoming empty histories") {
        runTest {
            RepositoryFixture(status = HttpStatusCode.BadRequest).use { fixture ->
                shouldThrow<ClientRequestException> {
                    fixture.repository.getRateHistory(base = "USD", code = "EUR")
                }
            }
        }
    }

    test("a network failure is propagated to the caller") {
        runTest {
            val failure = IOException("offline")
            RepositoryFixture(failure = failure).use { fixture ->
                shouldThrow<IOException> {
                    fixture.repository.getRateHistory(base = "USD", code = "EUR")
                } shouldBe failure
            }
        }
    }

    test("resourceFlow turns a failed history load into Loading then Error") {
        runTest {
            RepositoryFixture(status = HttpStatusCode.ServiceUnavailable).use { fixture ->
                val states = resourceFlow {
                    fixture.repository.getRateHistory(base = "USD", code = "EUR")
                }.toList()

                states.size shouldBe 2
                states.first() shouldBe Resource.Loading
                states.last().shouldBeInstanceOf<Resource.Error>().throwable
                    .shouldBeInstanceOf<ServerResponseException>()
            }
        }
    }

    test("resourceFlow treats an empty history as Data") {
        runTest {
            RepositoryFixture(response = EmptyHistoryResponse).use { fixture ->
                val states = resourceFlow {
                    fixture.repository.getRateHistory(base = "USD", code = "EUR")
                }.toList()

                states.size shouldBe 2
                states.first() shouldBe Resource.Loading
                states.last().shouldBeInstanceOf<Resource.Data<*>>()
            }
        }
    }

    test("cancellation is propagated instead of becoming a resource error") {
        runTest {
            RepositoryFixture(failure = CancellationException("cancelled")).use { fixture ->
                shouldThrow<CancellationException> {
                    resourceFlow { fixture.repository.getRateHistory(base = "USD", code = "EUR") }.toList()
                }
            }
        }
    }

    test("a malformed response fails instead of being reported as an empty history") {
        runTest {
            RepositoryFixture(response = "{}").use { fixture ->
                val states = resourceFlow {
                    fixture.repository.getRateHistory(base = "USD", code = "EUR")
                }.toList()

                states.last().shouldBeInstanceOf<Resource.Error>()
            }
        }
    }

})

private class RepositoryFixture(
    instant: String = "2026-11-04T12:00:00Z",
    response: String = HistoryResponse,
    status: HttpStatusCode = HttpStatusCode.OK,
    failure: Exception? = null,
    latestResponse: String = FallbackResponse,
    latestStatus: HttpStatusCode = HttpStatusCode.OK,
) : AutoCloseable {

    val clock = TestClock(Instant.parse(instant))

    val engine = MockEngine { request ->
        val (content, responseStatus) = when (request.url.encodedPath) {
            "/v1/currencies" -> {
                """{"EUR":"Euro"}""" to HttpStatusCode.OK
            }
            "/v1/latest" -> {
                latestResponse to latestStatus
            }
            else -> {
                if (failure != null) throw failure
                response to status
            }
        }
        respond(
            content = content,
            status = responseStatus,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private val client = HttpClient(engine) { configureHttpClient() }

    val repository = RatesRepositoryImpl(api = FrankfurterApi(client), converter = RatesConverter(), clock = clock)

    override fun close() {
        client.close()
    }

}

private class TestClock(var instant: Instant) : Clock {

    var readCount: Int = 0
        private set

    override fun now(): Instant {
        readCount += 1
        return instant
    }

}

private const val HistoryResponse = """
    {
      "base": "USD", "start_date": "2026-10-06", "end_date": "2026-11-04",
      "rates": {
        "2026-11-04": {"EUR": 0.91, "JPY": 160},
        "2026-10-05": {"EUR": 0.88},
        "2026-10-06": {"EUR": 0.89},
        "2026-11-05": {"EUR": 0.92}
      }
    }
"""

private const val EmptyHistoryResponse = """
    {"base": "USD", "start_date": "2026-10-06", "end_date": "2026-11-04", "rates": {}}
"""

private const val LatestRatesResponse = """
    {
      "base": "USD", "start_date": "2026-10-28", "end_date": "2026-11-03",
      "rates": {
        "2026-11-03": {"EUR": 0.91},
        "2026-10-30": {"EUR": 0.89},
        "2026-11-02": {"EUR": 0.90}
      }
    }
"""

private const val FallbackResponse = """
    {"amount": 1.0, "base": "USD", "date": "2026-11-03", "rates": {"JPY": 160, "EUR": 0.91}}
"""
