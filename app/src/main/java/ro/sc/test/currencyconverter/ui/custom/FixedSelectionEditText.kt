package ro.sc.test.currencyconverter.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import ro.sc.test.currencyconverter.R


class FixedSelectionEditText : AppCompatEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.editTextStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                requestFocus()
                performClick()
                return true
            }

            MotionEvent.ACTION_UP -> {
                setSelection(text?.length ?: 0)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        setSelection(text?.length ?: 0)
    }

}