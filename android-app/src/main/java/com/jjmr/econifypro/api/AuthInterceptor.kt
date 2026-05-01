package com.jjmr.econifypro.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(context: Context) : Interceptor {
    private val sharedPref = context.getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sharedPref.getString("access_token", null)
        val requestBuilder = chain.request().newBuilder()

        // Si tenemos el token guardado, lo inyectamos en la cabecera
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return chain.proceed(requestBuilder.build())
    }
}