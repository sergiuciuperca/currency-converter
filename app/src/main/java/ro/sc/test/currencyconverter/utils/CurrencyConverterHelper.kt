package ro.sc.test.currencyconverter.utils

object CurrencyConverterHelper {
    fun computeValuesByRate(
        baseCurrency: String,
        selectedCurrency: String,
        selectedCurrencyValue: Double,
        rates: Map<String, Double>
    ): Map<String, Double> {
        val convertedValues = mutableMapOf<String, Double>()
        val rate = rates[selectedCurrency] ?: 0.0
        val currencyValue = if (baseCurrency == selectedCurrency) selectedCurrencyValue else {
            if (rate > 0) {
                selectedCurrencyValue / rate
            } else
                0.0
        }

        for ((key, cRate) in rates) {
            convertedValues[key] = currencyValue * cRate
        }

        convertedValues[selectedCurrency] = selectedCurrencyValue
        if (baseCurrency != selectedCurrency) {
            convertedValues[baseCurrency] = selectedCurrencyValue * rate
        }

        return convertedValues
    }
}