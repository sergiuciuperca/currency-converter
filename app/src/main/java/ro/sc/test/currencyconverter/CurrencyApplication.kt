package ro.sc.test.currencyconverter

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import ro.sc.test.currencyconverter.di.DaggerAppComponent

class CurrencyApplication: DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out CurrencyApplication> {
        return DaggerAppComponent.factory().create(this)
    }
}