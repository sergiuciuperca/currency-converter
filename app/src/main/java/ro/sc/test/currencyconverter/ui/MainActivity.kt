package ro.sc.test.currencyconverter.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ro.sc.test.currencyconverter.R
import ro.sc.test.currencyconverter.ui.custom.hideKeyboard
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: MainViewModel

    private val currencyAdapter: CurrencyAdapter = CurrencyAdapter()
    private val layoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
    }

    private val scrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        recycler.apply {
            layoutManager = this@MainActivity.layoutManager
            adapter = currencyAdapter
            itemAnimator?.changeDuration = 0
            addOnScrollListener(scrollListener)
        }

        currencyAdapter.callbacks = object : CurrencyAdapter.Callbacks {
            override fun onValueChanged(value: CharSequence) {
                viewModel.onInputValueChanged(value.toString())
            }

            override fun onCurrencyClicked(currency: String) {
                viewModel.onCurrencyClicked(currency)
            }

        }

        viewModel.itemData().observe(this, Observer {
            if (it.scrollToTop) {
                currencyAdapter.submitList(it.currencies, Runnable {
                    layoutManager.scrollToPosition(0)
                })
            } else {
                currencyAdapter.submitList(it.currencies)
            }
        })


        viewModel.snackBarData().observe(this, Observer {
            if (it) {
                val snackbar =
                    Snackbar.make(coordinator, R.string.error, Snackbar.LENGTH_INDEFINITE)
                snackbar.setAction(R.string.retry, View.OnClickListener {
                    viewModel.retry()
                })

                snackbar.show()
            }
        })

        viewModel.currencyData().observe(this, Observer {
            currencyAdapter.updateCurrenciesValues(it)
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.onLifecycleEvent(false)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onLifecycleEvent(true)
    }

    override fun onDestroy() {
        currencyAdapter.clear()
        super.onDestroy()
    }
}
