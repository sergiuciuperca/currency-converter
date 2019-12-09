package ro.sc.test.currencyconverter.ui

import android.util.Log
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import ro.sc.test.currencyconverter.repo.CurrencyRepo
import ro.sc.test.currencyconverter.repo.data.CurrencyRatesUpdate
import ro.sc.test.currencyconverter.ui.data.CurrencyData
import ro.sc.test.currencyconverter.utils.CurrencyConverterHelper
import ro.sc.test.currencyconverter.utils.PreferencesManager
import ro.sc.test.currencyconverter.utils.ResourceUtils
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val currencyRepo: CurrencyRepo,
    private val resourceUtils: ResourceUtils,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val mutableState: MutableLiveData<UIUpdate> = MutableLiveData()
    private val errorLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private val disposables: CompositeDisposable = CompositeDisposable()
    private val uiEvents = PublishSubject.create<UIEvents>()
    private val decimalFormat = DecimalFormat("#.##")

    fun uiData(): LiveData<UIUpdate> = mutableState
    fun snackBarData(): LiveData<Boolean> = errorLiveData

    init {
        val currenciesOrder = preferencesManager.getCurrenciesList()
        val initialState = when {
            currenciesOrder != null && currenciesOrder.isNotEmpty() -> {
                MainViewState(
                    baseCurrency = currenciesOrder.first(),
                    selectedCurrency = currenciesOrder.first(),
                    uiData = UIUpdate(currencies = currenciesOrder.map {
                        CurrencyData(
                            title = it
                        )
                    })
                )
            }
            else -> {
                MainViewState()
            }
        }

        val currencySelectedEvent = uiEvents.ofType(UIEvents.CurrencySelected::class.java)
            .toFlowable(BackpressureStrategy.LATEST)
            .map { it.currency }
            .share()

        val currencyRateFlowable =
            Flowable.combineLatest(currencySelectedEvent.startWith(initialState.baseCurrency),
                uiEvents.ofType(UIEvents.LifecycleEvent::class.java)
                    .toFlowable(BackpressureStrategy.LATEST)
                    .map { it.viewPaused },
                BiFunction<String, Boolean, Pair<String, Boolean>> { currency, viewPaused ->
                    Pair(currency, viewPaused)
                })
                .switchMap { currencyLifecyclePair ->
                    when {
                        currencyLifecyclePair.second == false -> {
                            return@switchMap currencyRepo.getCurrentRates(currencyLifecyclePair.first.orEmpty())
                                .map { Result.UpdatedRate(it) }
                        }
                        else -> {
                            return@switchMap Flowable.empty<Result>()
                        }
                    }
                }

        val valueObserver = uiEvents.ofType(UIEvents.ValueChanged::class.java)
            .toFlowable(BackpressureStrategy.LATEST)
            .map { Result.NewValue(it.value) }

        val disposable =
            Flowable.merge(
                listOf(
                    currencyRateFlowable,
                    currencySelectedEvent.map { Result.NewCurrency(it) as Result },
                    valueObserver
                )
            )
                .observeOn(Schedulers.computation())
                .scan(initialState, BiFunction { oldState, result ->
                    val newState = when (result) {
                        is Result.NewValue -> {
                            oldState.copy(
                                selectedCurrencyValue = result.value.toDoubleOrNull() ?: 0.0,
                                selectedCurrencyStringValue = result.value,
                                showSnackbar = false
                            )
                        }
                        is Result.UpdatedRate -> {
                            when {
                                result.currencyRatesUpdate.error == null -> {
                                    oldState.copy(
                                        baseCurrency = result.currencyRatesUpdate.base,
                                        rates = result.currencyRatesUpdate.rates,
                                        showSnackbar = false
                                    )
                                }
                                else -> {
                                    oldState.copy(
                                        showSnackbar = true
                                    )
                                }
                            }
                        }

                        is Result.NewCurrency -> {
                            val currencyValue =
                                (oldState.uiData.currencies.firstOrNull { it.title == result.currency }?.value
                                    ?: "0").toDouble()
                            oldState.copy(
                                selectedCurrency = result.currency,
                                selectedCurrencyValue = currencyValue,
                                selectedCurrencyStringValue = decimalFormat.format(currencyValue),
                                showSnackbar = false
                            )
                        }
                    }

                    val oldIndexes = mutableMapOf<String, Int>()
                    oldState.uiData.currencies.forEachIndexed { index, currencyData ->
                        oldIndexes[currencyData.title] = index
                    }

                    val oldCurrentSelectedIndex = oldIndexes[newState.selectedCurrency] ?: 0
                    oldIndexes[newState.selectedCurrency] = 0

                    for (i in 0 until oldCurrentSelectedIndex) {
                        oldIndexes[oldState.uiData.currencies[i].title] = i + 1
                    }

                    val convertedValues = CurrencyConverterHelper.computeValuesByRate(
                        newState.baseCurrency,
                        newState.selectedCurrency,
                        newState.selectedCurrencyValue,
                        newState.rates
                    )

                    val newRates = convertedValues.map {
                        val currency = Currency.getInstance(it.key)
                        when {
                            it.key == newState.selectedCurrency -> {
                                return@map CurrencyData(
                                    it.key,
                                    currency.displayName,
                                    newState.selectedCurrencyStringValue,
                                    resourceUtils.getDrawableResourceId(currency.currencyCode)
                                )
                            }
                            else -> {
                                val formatedValue = decimalFormat.format(it.value)
                                val dotIndex = formatedValue.indexOf('.')
                                val newValue = when {
                                    newState.selectedCurrencyValue == 0.0 -> ""
                                    dotIndex > -1 && dotIndex == formatedValue.length - 2 -> formatedValue + "0"
                                    else -> formatedValue
                                }
                                return@map CurrencyData(
                                    it.key,
                                    currency.displayName,
                                    newValue,
                                    resourceUtils.getDrawableResourceId(currency.currencyCode)
                                )
                            }
                        }

                    }

                    val uiUpdate = UIUpdate(newRates.sortedBy {
                        oldIndexes[it.title] ?: Int.MAX_VALUE
                    }, result is Result.NewCurrency)

                    return@BiFunction newState.copy(uiData = uiUpdate)
                })
                .doOnNext { state ->
                    val currenciesInOrder = state.uiData.currencies.map { it.title }
                    preferencesManager.saveCurrentCurrenciesList(currenciesInOrder)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mutableState.value = it.uiData
                    errorLiveData.value = it.showSnackbar
                }, {
                    Log.e("MainVM", "Unhandled error", it)
                })

        disposables.add(disposable)
    }

    fun onInputValueChanged(value: String) {
        uiEvents.onNext(UIEvents.ValueChanged(value))
    }

    fun onCurrencyClicked(currency: String) {
        uiEvents.onNext(UIEvents.CurrencySelected(currency))
    }

    fun onLifecycleEvent(viewPaused: Boolean) {
        uiEvents.onNext(UIEvents.LifecycleEvent(viewPaused))
    }

    override fun onCleared() {
        disposables.clear()
        super.onCleared()
    }

    fun retry() {
        val currency = mutableState.value?.currencies?.firstOrNull()?.title ?: return
        uiEvents.onNext(UIEvents.CurrencySelected(currency))
    }

    private sealed class UIEvents {
        data class CurrencySelected(val currency: String) : UIEvents()
        data class ValueChanged(val value: String) : UIEvents()
        data class LifecycleEvent(val viewPaused: Boolean) : UIEvents()
    }


    private sealed class Result {
        data class NewValue(val value: String) : Result()
        data class NewCurrency(val currency: String) : Result()
        data class UpdatedRate(val currencyRatesUpdate: CurrencyRatesUpdate) : Result()
    }

    data class MainViewState(
        val baseCurrency: String = "EUR",
        val selectedCurrency: String = "EUR",
        val selectedCurrencyValue: Double = 1.0,
        val selectedCurrencyStringValue: String = "1",
        val rates: Map<String, Double> = emptyMap(),
        val uiData: UIUpdate = UIUpdate(),
        val showSnackbar: Boolean = false
    )

    data class UIUpdate(
        val currencies: List<CurrencyData> = emptyList(),
        val scrollToTop: Boolean = false
    )
}