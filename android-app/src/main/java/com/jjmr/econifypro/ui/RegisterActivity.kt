package com.jjmr.econifypro.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.R
import com.jjmr.econifypro.model.UserRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : BaseActivity() {
    private lateinit var nameInput: EditText
    private lateinit var lastnameInput: EditText // Añadido para apellidos
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var goToLogin: TextView
    private lateinit var loginProgress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        nameInput = findViewById(R.id.nameInput)
        lastnameInput = findViewById(R.id.lastnameInput) // Vinculamos el nuevo ID
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerButton = findViewById(R.id.registerButton)
        goToLogin = findViewById(R.id.goToLogin)
        loginProgress = findViewById(R.id.loginProgress)
    }

    private fun setupListeners() {
        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val firstname = nameInput.text.toString().trim()
            val lastname = lastnameInput.text.toString().trim() // Capturamos apellidos reales

            // 1. Validaciones locales
            if (email.isEmpty() || password.isEmpty() || firstname.isEmpty() || lastname.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) { // Un extra de seguridad para tu memoria
                passwordInput.error = "La contraseña debe tener al menos 6 caracteres"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordInput.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            if (!isEmailValid(email)) {
                emailInput.error = "Introduce un email válido"
                return@setOnClickListener
            }

            setLoading(true, registerButton, loginProgress)

            // 2. Llamada al Backend
            val userRequest = UserRequest(
                email = email,
                password = password,
                firstname = firstname,
                lastname = lastname
            )

            NetworkConfig.getApiService(this).register(userRequest).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    setLoading(false, registerButton, loginProgress)
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "¡Bienvenido, $firstname! Cuenta creada.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        // Aquí manejamos el error 400 que configuramos en Django
                        Toast.makeText(this@RegisterActivity, "El email ya está en uso o los datos son inválidos", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    setLoading(false, registerButton, loginProgress)
                    Toast.makeText(this@RegisterActivity, "Error de conexión: Verifica tu servidor", Toast.LENGTH_SHORT).show()
                }
            })
        }

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}