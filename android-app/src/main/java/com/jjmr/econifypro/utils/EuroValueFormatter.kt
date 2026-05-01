package com.jjmr.econifypro.utils
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class EuroValueFormatter : ValueFormatter() {

    // Configuramos el formato una sola vez para ser eficientes
    private val decimalFormat: DecimalFormat = {
        val symbols = DecimalFormatSymbols(Locale("es", "ES"))
        symbols.groupingSeparator = '.'
        symbols.decimalSeparator = ','

        val df = DecimalFormat("#,##0.00", symbols)
        df
    }()

    override fun getFormattedValue(value: Float): String {
        return "${decimalFormat.format(value)}€"
    }
}