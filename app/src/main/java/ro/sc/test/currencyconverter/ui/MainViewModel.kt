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
import ro.sc.test.currencyconverter.ui.data.CurrencyItem
import ro.sc.test.currencyconverter.utils.CurrencyConverterHelper
import ro.sc.test.currencyconverter.utils.PreferencesManager
import ro.sc.test.currencyconverter.utils.ResourceUtils
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val currencyRepo: CurrencyRepo,
    private val resourceUtils: ResourceUtils,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val mutableState: MutableLiveData<ItemsUpdate> = MutableLiveData()
    private val currenciesValueState: MutableLiveData<Map<String, String>> = MutableLiveData()
    private val errorLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private val disposables: CompositeDisposable = CompositeDisposable()
    private val uiEvents = PublishSubject.create<UIEvents>()
    private val decimalFormat: DecimalFormat by lazy {
        val format = DecimalFormat("#.##")
//        format.roundingMode = RoundingMode.FLOOR
        return@lazy format
    }

    fun itemData(): LiveData<ItemsUpdate> = mutableState
    fun currencyData(): LiveData<Map<String, String>> = currenciesValueState
    fun snackBarData(): LiveData<Boolean> = errorLiveData

    init {
        val currenciesOrder = preferencesManager.getCurrenciesList()
        val initialState = when {
            currenciesOrder != null && currenciesOrder.isNotEmpty() -> {
                MainViewState(
                    baseCurrency = currenciesOrder.first(),
                    selectedCurrency = currenciesOrder.first()
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
                    currencySelectedEvent.map { Result.NewCurrency(it) as Result }
                        .startWith(
                            Result.InitialCurrencies(
                                currenciesOrder.orEmpty()
                            )
                        ),
                    valueObserver
                )
            )
                .observeOn(Schedulers.computation())
                .scan(initialState, BiFunction { oldState, result ->
                    val newState = when (result) {
                        is Result.InitialCurrencies -> {
                            val items = result.currencies.map {
                                val currency = Currency.getInstance(it)
                                return@map CurrencyItem(
                                    it,
                                    currency.displayName,
                                    resourceUtils.getDrawableResourceId(it)
                                )
                            }
                            oldState.copy(
                                currencyItems = items,
                                itemsData = ItemsUpdate(
                                    currencies = items
                                )
                            )
                        }
                        is Result.NewValue -> {
                            oldState.copy(
                                selectedCurrencyValue = result.value.toDoubleOrNull() ?: 0.0,
                                selectedCurrencyStringValue = result.value,
                                itemsData = null,
                                showSnackbar = false
                            )
                        }
                        is Result.UpdatedRate -> {
                            when {
                                result.currencyRatesUpdate.error == null -> {
                                    val currencyItems = if (oldState.currencyItems.isEmpty()) {
                                        result.currencyRatesUpdate.rates.keys.map {
                                            val currency = Currency.getInstance(it)
                                            return@map CurrencyItem(
                                                it,
                                                currency.displayName,
                                                resourceUtils.getDrawableResourceId(it)
                                            )
                                        }
                                            .toMutableList()
                                            .also {
                                                val currency =
                                                    Currency.getInstance(result.currencyRatesUpdate.base)
                                                it.add(
                                                    0,
                                                    CurrencyItem(
                                                        result.currencyRatesUpdate.base,
                                                        currency.displayName,
                                                        resourceUtils.getDrawableResourceId(result.currencyRatesUpdate.base)
                                                    )
                                                )
                                            }
                                    } else {
                                        null
                                    }


                                    oldState.copy(
                                        baseCurrency = result.currencyRatesUpdate.base,
                                        rates = result.currencyRatesUpdate.rates,
                                        currencyItems = currencyItems ?: oldState.currencyItems,
                                        itemsData = if (currencyItems != null) ItemsUpdate(
                                            currencyItems
                                        ) else null,
                                        showSnackbar = false
                                    )
                                }
                                else -> {
                                    oldState.copy(
                                        itemsData = null,
                                        showSnackbar = true
                                    )
                                }
                            }
                        }

                        is Result.NewCurrency -> {
                            val items = oldState.currencyItems.toMutableList()
                            val indexOfNewCurrency =
                                items.indexOfFirst { it.title == result.currency }
                            if (indexOfNewCurrency > 0) {
                                val currency = items.removeAt(indexOfNewCurrency)
                                items.add(0, currency)
                            }
                            val currencyValue =
                                oldState.currenciesData[result.currency]?.toDoubleOrNull() ?: 0.0
                            oldState.copy(
                                selectedCurrency = result.currency,
                                selectedCurrencyValue = currencyValue,
                                selectedCurrencyStringValue = decimalFormat.format(currencyValue),
                                currencyItems = items,
                                itemsData = ItemsUpdate(items, true),
                                showSnackbar = false
                            )
                        }
                    }

                    val convertedValues = CurrencyConverterHelper.computeValuesByRate(
                        newState.baseCurrency,
                        newState.selectedCurrency,
                        newState.selectedCurrencyValue,
                        newState.rates
                    )

                    val currencyData = convertedValues.map {
                        if (it.key == newState.selectedCurrency) {
                            it.key to newState.selectedCurrencyStringValue
                        } else {
                            val formattedValue = decimalFormat.format(it.value)
                            val dotIndex = formattedValue.indexOf('.')
                            val newValue = when {
                                newState.selectedCurrencyValue == 0.0 -> ""
                                dotIndex > -1 && dotIndex == formattedValue.length - 2 -> formattedValue + "0"
                                else -> formattedValue
                            }

                            it.key to newValue
                        }
                    }.toMap()

                    return@BiFunction newState.copy(
                        currenciesData = currencyData
                    )
                })
                .doOnNext { state ->
                    val currenciesInOrder = state.currencyItems.map { it.title }
                    preferencesManager.saveCurrentCurrenciesList(currenciesInOrder)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ state ->
                    state.itemsData?.let {
                        mutableState.value = it
                    }
                    currenciesValueState.value = state.currenciesData
                    errorLiveData.value = state.showSnackbar
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
        data class InitialCurrencies(val currencies: List<String>) : Result()
    }

    data class MainViewState(
        val baseCurrency: String = "EUR",
        val selectedCurrency: String = "EUR",
        val selectedCurrencyValue: Double = 1.0,
        val selectedCurrencyStringValue: String = "1",
        val rates: Map<String, Double> = emptyMap(),
        val currenciesData: Map<String, String> = emptyMap(),
        val currencyItems: List<CurrencyItem> = emptyList(),
        val itemsData: ItemsUpdate? = null,
        val showSnackbar: Boolean = false
    )

    data class ItemsUpdate(
        val currencies: List<CurrencyItem> = emptyList(),
        val scrollToTop: Boolean = false
    )
}