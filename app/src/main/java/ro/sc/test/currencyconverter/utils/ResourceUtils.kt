package ro.sc.test.currencyconverter.utils

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ResourceUtils @Inject constructor(private val context: Context) {
    fun getDrawableResourceId(currencyCode: String): Int {
        val res = context.resources
        return res.getIdentifier(
            "ic_${currencyCode.toLowerCase()}",
            "drawable",
            context.packageName
        )
    }
}