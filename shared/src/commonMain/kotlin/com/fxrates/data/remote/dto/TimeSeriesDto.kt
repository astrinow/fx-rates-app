package com.fxrates.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /v1/{start}..{end}` — the rates keyed by ISO date, then by currency code. */
@Serializable
data class TimeSeriesDto(
    @SerialName("base") val base: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("rates") val rates: Map<String, Map<String, Double>>,
)
