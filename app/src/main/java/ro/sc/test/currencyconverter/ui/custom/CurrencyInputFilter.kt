package ro.sc.test.currencyconverter.ui.custom

import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import java.util.regex.Pattern

class CurrencyInputFilter : InputFilter {
    private val pattern: Pattern = Pattern.compile("[0-9]{0,9}+((\\.[0-9]?)?)|(\\.)?")
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val matcher = pattern.matcher(dest)

        val dotPos = dest?.indexOf('.') ?: return ""

        if (!matcher.matches()) {
            if (dotPos < 0 || dstart > dotPos ||
                source.isNullOrBlank() || dotPos >= 7
            ) {
                return ""
            }
            if ((source == "0" && (dstart == 0 && dotPos >= 1)) ||
                (dstart == 1 && dest[0] == '0')
            ) {
                return ""
            }

            return null
        } else {
            if (dstart == 1 && dest.firstOrNull() == '0' && ((source?.length ?: 0) > 0 && source?.firstOrNull() != '.')) {
                return ""
            }
            return null
        }

    }
}