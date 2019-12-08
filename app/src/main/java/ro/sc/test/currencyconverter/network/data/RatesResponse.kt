package ro.sc.test.currencyconverter.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RatesResponse(
    @SerialName("base") val base: String,
    @SerialName("rates") val ratesxxxxx: Map<String, Double>
)