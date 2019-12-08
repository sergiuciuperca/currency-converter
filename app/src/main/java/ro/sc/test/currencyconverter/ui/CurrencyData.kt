package ro.sc.test.currencyconverter.ui

data class CurrencyData(
    val title: String,
    val description: String = "",
    val value: String = "0",
    val resourceId: Int = 0
)