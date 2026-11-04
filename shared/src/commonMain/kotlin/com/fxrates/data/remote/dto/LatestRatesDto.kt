package com.fxrates.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** `GET /v1/latest` — quotes for the last published business day. */
@Serializable
data class LatestRatesDto(
    @SerialName("base") val base: String,
    @SerialName("date") val date: String,
    @SerialName("rates") val rates: Map<String, JsonPrimitive>,
)
