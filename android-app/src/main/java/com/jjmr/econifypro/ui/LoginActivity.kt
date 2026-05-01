package com.jjmr.econifypro.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ProgressBar
import com.jjmr.econifypro.model.LoginRequest
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.R
import com.jjmr.econifypro.model.LoginResponse
import com.jjmr.econifypro.utils.showCustomSnackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val goToRegister = findViewById<TextView>(R.id.goToRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val loginProgress = findViewById<ProgressBar>(R.id.loginProgress)

        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passwordInput.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                // Ahora usamos la extensión directamente
                showCustomSnackbar("Por favor, completa tus credenciales para entrar.")
                return@setOnClickListener
            }

            setLoading(true, loginButton, loginProgress)
            val request = LoginRequest(email, pass)

            NetworkConfig.getApiService(this).login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    setLoading(false, loginButton, loginProgress)

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("DEBUG_LOGIN", "Respuesta del server - Q1: ${body?.securityQuestion1}, A1: ${body?.securityAnswer1}")

                        val sharedPref = getSharedPreferences("EconifyPrefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("access_token", body?.access)
                            putString("user_firstname", body?.firstname ?: "Usuario")
                            putString("user_lastname", body?.lastname ?: "")
                            putString("user_q1", body?.securityQuestion1?.toString())
                            putString("user_a1", body?.securityAnswer1)
                            putString("user_q2", body?.securityQuestion2?.toString())
                            putString("user_a2", body?.securityAnswer2)
                            apply()
                        }

                        // Mensaje unificado
                        showCustomSnackbar("¡Bienvenido a Econify!")

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        showCustomSnackbar("El correo o la contraseña no parecen ser correctos.")
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    setLoading(false, loginButton, loginProgress)
                    showCustomSnackbar("Parece que hay un problema con la red. Inténtalo de nuevo.")
                }
            })
        }
    }
}