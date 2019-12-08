package ro.sc.test.currencyconverter.repo

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import ro.sc.test.currencyconverter.network.CurrencyService
import ro.sc.test.currencyconverter.repo.data.CurrencyRatesUpdate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepo @Inject constructor(private val currencyService: CurrencyService) {

    fun getCurrentRates(base: String): Flowable<CurrencyRatesUpdate> {
        return Flowable.interval(1, TimeUnit.SECONDS, Schedulers.io())
            .startWith(-1)
            .observeOn(Schedulers.io())
            .switchMapSingle { _ ->
                currencyService.getCurrentRates(base)
                    .map { CurrencyRatesUpdate(it.base, it.ratesxxxxx) }
            }
            .onErrorReturn { CurrencyRatesUpdate(error = it) }
    }
}