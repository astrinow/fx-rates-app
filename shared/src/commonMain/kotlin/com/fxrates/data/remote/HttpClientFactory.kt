package com.fxrates.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ClientRequestException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val FrankfurterBaseUrl = "https://api.frankfurter.dev/v1/"

internal expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

internal fun createHttpClient(): HttpClient = platformHttpClient {
    expectSuccess = true
    defaultRequest {
        url(FrankfurterBaseUrl)
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }
}
