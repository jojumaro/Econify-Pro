package com.jjmr.econifypro.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jjmr.econifypro.R
class MainActivity : BaseActivity() { // Heredamos de tu BaseActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Usamos el nuevo layout

        val sharedPref = getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("access_token", null)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 1. Cargar el Dashboard por defecto al entrar
        if (savedInstanceState == null) {
            cambiarFragmento(DashboardFragment())
        }

        // 2. Escuchar los clics de la barra inferior
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    cambiarFragmento(DashboardFragment())
                    true
                }
                R.id.nav_transactions -> {
                    cambiarFragmento(TransactionsFragment())
                    true
                }
                R.id.nav_goals -> {
                    cambiarFragmento(GoalsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun cambiarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }
}