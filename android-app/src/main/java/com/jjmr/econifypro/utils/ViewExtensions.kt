package com.jjmr.econifypro.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.jjmr.econifypro.R

// 1. Para tus FRAGMENTS (como Dashboard o Transactions)
fun Fragment.showSnackbar(message: String) {
    val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)

    // Buscamos la bottom_nav en la actividad que contiene al fragment
    val bottomNav = activity?.findViewById<View>(R.id.bottom_navigation)
    if (bottomNav != null) {
        snackbar.anchorView = bottomNav
    }
    snackbar.show()
}

// 2. Para tus ACTIVITIES (como LoginActivity)
fun AppCompatActivity.showCustomSnackbar(message: String) {
    val rootView = findViewById<View>(android.R.id.content)
    val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)

    val bottomNav = findViewById<View>(R.id.bottom_navigation)
    if (bottomNav != null) {
        snackbar.anchorView = bottomNav
    }
    snackbar.show()
}