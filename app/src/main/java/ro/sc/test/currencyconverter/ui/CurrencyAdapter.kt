package ro.sc.test.currencyconverter.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.item_currency.view.*
import ro.sc.test.currencyconverter.R
import ro.sc.test.currencyconverter.ui.custom.CurrencyInputFilter
import ro.sc.test.currencyconverter.ui.custom.showKeyboard
import ro.sc.test.currencyconverter.ui.data.CurrencyItem


class CurrencyAdapter :
    ListAdapter<CurrencyItem, CurrencyAdapter.CurrencyItemViewHolder>(CurrencyDiffCallbacks()) {
    var callbacks: Callbacks? = null
    private val inputFilter = CurrencyInputFilter()
    private val convertedCurrenciesSubject: BehaviorSubject<Map<String, String>> = BehaviorSubject.create()

    private val itemClickListener = View.OnClickListener { view ->
        val currency = view.getTag(R.id.tag_currency) as? String ?: return@OnClickListener
        callbacks?.onCurrencyClicked(currency)
        view.input_value.showKeyboard()
    }

    private val focusListener = View.OnFocusChangeListener { view, hasFocus ->
        val editText = view as? EditText

        when {
            hasFocus -> {
                editText?.addTextChangedListener(textChangeListener)
                editText?.filters = arrayOf(inputFilter)
            }
            else -> {
                editText?.filters = emptyArray()
                editText?.removeTextChangedListener(textChangeListener)
            }
        }
    }

    private val textChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            callbacks?.onValueChanged(s ?: "")
        }
    }

    interface Callbacks {
        fun onCurrencyClicked(currency: String)
        fun onValueChanged(value: CharSequence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyItemViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_currency, parent, false)
        view.setOnClickListener(itemClickListener)
        view.input_value.setOnClickListener(itemClickListener)
        view.input_value.onFocusChangeListener = focusListener

        return CurrencyItemViewHolder(view)
    }


    override fun onBindViewHolder(holder: CurrencyItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setTag(R.id.tag_currency, item.title)
        holder.value.setTag(R.id.tag_currency, item.title)

//        Log.e("position", "position is $position")
        holder.title.text = item.title
        holder.description.text = item.description
        holder.icon.setImageResource(item.resourceId)
        holder.observeValueOf(
            convertedCurrenciesSubject
                .toFlowable(BackpressureStrategy.LATEST).map {
                    it[item.title] ?: ""
                }
                .distinctUntilChanged()
        )
    }

    override fun onViewRecycled(holder: CurrencyItemViewHolder) {
        holder.stopObserving()
    }


    fun updateCurrenciesValues(newValues: Map<String, String>) {
        convertedCurrenciesSubject.onNext(newValues)
    }

    fun clear() {
        convertedCurrenciesSubject.onComplete()
    }


    class CurrencyItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon = view.icon!!
        val title = view.title!!
        val description = view.description!!
        val value = view.input_value!!

        private var disposable: Disposable? = null

        fun observeValueOf(valueFlowable: Flowable<String>) {
            disposable = valueFlowable.filter { !value.isFocused }
                .subscribe { newValue ->
                    value.text.also {
                        it?.replace(0, it.length, newValue)
                    }
                }
        }

        fun stopObserving() {
            disposable?.dispose()
        }
    }

    class CurrencyDiffCallbacks : DiffUtil.ItemCallback<CurrencyItem>() {
        override fun areItemsTheSame(oldItem: CurrencyItem, newItem: CurrencyItem): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: CurrencyItem, newItem: CurrencyItem): Boolean {
            return oldItem == newItem
        }

    }
}