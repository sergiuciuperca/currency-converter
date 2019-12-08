package ro.sc.test.currencyconverter.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(private val sharedPreferences: SharedPreferences) {
    companion object {
        const val currenciesOrder = "CURRENCIES_ORDER"
    }

    public fun saveCurrentCurrenciesList(currenciesList: List<String>) {
        sharedPreferences.edit {
            putString(currenciesOrder, currenciesList.joinToString(";"))
        }
    }

    public fun getCurrenciesList(): List<String>? {
        val stringValue = sharedPreferences.getString(currenciesOrder, null) ?: return null
        return stringValue.split(";")
    }
}