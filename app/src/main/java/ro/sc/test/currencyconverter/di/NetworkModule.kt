package ro.sc.test.currencyconverter.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.MediaType
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import ro.sc.test.currencyconverter.BuildConfig
import ro.sc.test.currencyconverter.network.CurrencyService
import javax.inject.Singleton

@Module
class NetworkModule {


    @Singleton
    @Provides
    fun provideCurrencyService(): CurrencyService {
        val contentType = MediaType.get("application/json")

        return Retrofit.Builder()
            .baseUrl(BuildConfig.CURRENCY_ENPOINT)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(Json(JsonConfiguration(strictMode = false)).asConverterFactory(contentType))
            .build()
            .create(CurrencyService::class.java)
    }
}