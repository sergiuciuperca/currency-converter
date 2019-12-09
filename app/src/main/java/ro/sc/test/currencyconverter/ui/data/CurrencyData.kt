package ro.sc.test.currencyconverter.ui.data

data class CurrencyData(
    val title: String,
    val description: String = "",
    val value: String = "0",
    val resourceId: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        val other = other as? CurrencyData ?: return false
        return this.title == other.title &&
                this.description == other.description &&
                this.resourceId == other.resourceId
    }
}