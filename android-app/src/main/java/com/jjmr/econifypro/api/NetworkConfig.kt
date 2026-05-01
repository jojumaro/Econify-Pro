package com.jjmr.econifypro.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkConfig {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    // Ahora creamos una función para inicializar el servicio con el contexto
    fun getApiService(context: Context): ApiService {

        // 1. Creamos el cliente de red con nuestro "guardaespaldas" (Interceptor)
        val client = OkHttpClient.Builder()
            //.addInterceptor(AuthInterceptor(context))
            .build()

        // 2. Construimos Retrofit usando ese cliente
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // <--- Aquí es donde vinculamos la seguridad
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}