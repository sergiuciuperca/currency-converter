package ro.sc.test.currencyconverter.network

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query
import ro.sc.test.currencyconverter.network.data.RatesResponse

interface CurrencyService {
    @GET("latest")
    fun getCurrentRates(@Query("base") currency: String): Single<RatesResponse>
}