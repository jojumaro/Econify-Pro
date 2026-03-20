package com.jjmr.econifypro.ui

import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

// "open" permite que otras clases hereden de esta
open class BaseActivity : AppCompatActivity() {

    /**
     * Gestiona el estado de carga de forma global.
     * @param isLoading Si es true, oculta el botón y muestra el loader.
     */
    fun setLoading(isLoading: Boolean, button: Button, loader: ProgressBar) {
        if (isLoading) {
            button.visibility = View.GONE
            loader.visibility = View.VISIBLE
            // Deshabilitamos el botón por seguridad extra
            button.isEnabled = false
        } else {
            button.visibility = View.VISIBLE
            loader.visibility = View.GONE
            button.isEnabled = true
        }
    }
}