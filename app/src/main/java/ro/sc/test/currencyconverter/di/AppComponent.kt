package ro.sc.test.currencyconverter.di

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import ro.sc.test.currencyconverter.CurrencyApplication
import ro.sc.test.currencyconverter.ui.MainActivityBuilder
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        NetworkModule::class,
        ViewModelBuilder::class,
        MainActivityBuilder::class
    ]
)
interface AppComponent : AndroidInjector<CurrencyApplication> {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: CurrencyApplication): AndroidInjector<CurrencyApplication>
    }
}