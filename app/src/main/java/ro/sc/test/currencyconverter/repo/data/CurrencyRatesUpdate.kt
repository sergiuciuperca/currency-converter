package ro.sc.test.currencyconverter.repo.data

data class CurrencyRatesUpdate(
    val base: String = "",
    val rates: Map<String, Double> = emptyMap(),
    val error: Throwable? = null
)