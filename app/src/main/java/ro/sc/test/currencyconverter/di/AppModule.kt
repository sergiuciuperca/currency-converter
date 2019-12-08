package ro.sc.test.currencyconverter.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import ro.sc.test.currencyconverter.CurrencyApplication

@Module
class AppModule {
    @Provides
    fun provideContext(application: CurrencyApplication): Context = application.applicationContext

    @Provides
    fun provideSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
}