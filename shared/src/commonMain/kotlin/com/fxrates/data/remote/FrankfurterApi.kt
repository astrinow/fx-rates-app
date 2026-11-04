package com.fxrates.data.remote

import com.fxrates.data.remote.dto.TimeSeriesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate
import me.tatarka.inject.annotations.Inject

/** The Frankfurter API (https://frankfurter.dev): ECB reference rates, no key required. */
@Inject
class FrankfurterApi(
    private val httpClient: HttpClient,
) {

    /** Currency code → English name. */
    suspend fun getCurrencies(): Map<String, String> = httpClient.get("currencies").body()

    /**
     * The rates for every business day from [start] to [end] inclusive; `null` [end] means
     * "up to the latest available date".
     */
    suspend fun getTimeSeries(base: String, start: LocalDate, end: LocalDate? = null): TimeSeriesDto {
        val path = "$start..${end ?: ""}"
        return httpClient.get(path) {
            parameter("base", base)
        }.body()
    }

}
