package com.jjmr.econifypro.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jjmr.econifypro.R

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Recuperamos el nombre que guardamos en el SharedPreferences durante el Login
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
        val firstName = sharedPref.getString("user_firstname", "Usuario")

        // 2. Buscamos el TextView por el ID que tienes en tu XML
        val greetingText = view.findViewById<TextView>(R.id.dashboardGreeting)
        val avatarText = view.findViewById<TextView>(R.id.userAvatar)

        // 3. Ponemos los datos reales
        greetingText.text = "Hola, $firstName"
        avatarText.text = firstName?.firstOrNull()?.toString()?.uppercase() ?: "U"
    }
}