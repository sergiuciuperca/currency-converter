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
import kotlinx.android.synthetic.main.item_currency.view.*
import ro.sc.test.currencyconverter.R
import ro.sc.test.currencyconverter.ui.custom.CurrencyInputFilter
import ro.sc.test.currencyconverter.ui.custom.showKeyboard
import ro.sc.test.currencyconverter.ui.data.CurrencyData


class CurrencyAdapter :
    ListAdapter<CurrencyData, CurrencyAdapter.CurrencyItemViewHolder>(CurrencyDiffCallbacks()) {
    var callbacks: Callbacks? = null
    private val inputFilter = CurrencyInputFilter()

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
        holder.title.text = item.title
        holder.description.text = item.description
        holder.value.setText(item.value)
        holder.icon.setImageResource(item.resourceId)
    }


    class CurrencyItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon = view.icon!!
        val title = view.title!!
        val description = view.description!!
        val value = view.input_value!!
    }

    class CurrencyDiffCallbacks : DiffUtil.ItemCallback<CurrencyData>() {
        override fun areItemsTheSame(oldItem: CurrencyData, newItem: CurrencyData): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: CurrencyData, newItem: CurrencyData): Boolean {
            return oldItem == newItem
        }

    }
}