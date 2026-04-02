package com.jjmr.econifypro.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jjmr.econifypro.R

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Recuperamos el nombre de SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
        val firstName = sharedPref.getString("user_firstname", "Usuario")

        // 2. Enlazamos las vistas del nuevo XML
        val greetingText = view.findViewById<TextView>(R.id.dashboardGreeting)
        val btnSettings = view.findViewById<ImageView>(R.id.btnSettings)

        // 3. Ponemos el saludo real
        greetingText.text = "Hola, $firstName"

        // 4. Lógica para abrir el perfil al pulsar el engranaje
        btnSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, ProfileFragment()) // Asegúrate de tener creado ProfileFragment.kt
                .addToBackStack(null)
                .commit()
        }
    }
}