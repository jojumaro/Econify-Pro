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
import com.jjmr.econifypro.model.SecurityQuestion
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.util.Log

class RegisterActivity : BaseActivity() {
    private lateinit var nameInput: EditText
    private lateinit var lastnameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var goToLogin: TextView
    private lateinit var loginProgress: ProgressBar
    private lateinit var question1Spinner: Spinner
    private lateinit var question2Spinner: Spinner
    private lateinit var answer1Input: EditText
    private lateinit var answer2Input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()
        cargarPreguntas()
    }

    private fun initViews() {
        nameInput = findViewById(R.id.nameInput)
        lastnameInput = findViewById(R.id.lastnameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerButton = findViewById(R.id.registerButton)
        goToLogin = findViewById(R.id.goToLogin)
        loginProgress = findViewById(R.id.loginProgress)
        question1Spinner = findViewById(R.id.question1Spinner)
        question2Spinner = findViewById(R.id.question2Spinner)
        answer1Input = findViewById(R.id.answer1Input)
        answer2Input = findViewById(R.id.answer2Input)
    }

    private fun setupListeners() {
        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val firstname = nameInput.text.toString().trim()
            val lastname = lastnameInput.text.toString().trim()
            val q1 = question1Spinner.selectedItem as SecurityQuestion
            val q2 = question2Spinner.selectedItem as SecurityQuestion
            val ans1 = answer1Input.text.toString().trim()
            val ans2 = answer2Input.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || firstname.isEmpty() || lastname.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ans1.isEmpty() || ans2.isEmpty()) {
                Toast.makeText(this, "Responde a las preguntas de seguridad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
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

            val userRequest = UserRequest(
                email = email,
                password = password,
                firstname = firstname,
                lastname = lastname,
                security_question_1 = q1.id,
                security_answer_1 = ans1,
                security_question_2 = q2.id,
                security_answer_2 = ans2
            )

            NetworkConfig.getApiService(this).register(userRequest).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    setLoading(false, registerButton, loginProgress)
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "¡Bienvenido, $firstname! Cuenta creada.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
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

    private fun cargarPreguntas() {
        Log.d("DEBUG_API", "PASO 1: Entrando...")
        val service = NetworkConfig.getApiService(this)
        val call = service.getSecurityQuestions()

        call.enqueue(object : Callback<List<SecurityQuestion>> {
            override fun onResponse(call: Call<List<SecurityQuestion>>, response: Response<List<SecurityQuestion>>) {
                Log.d("DEBUG_API", "PASO 3: Respuesta recibida. Codigo: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val preguntas = response.body()!!
                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this@RegisterActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            preguntas
                        )
                        question1Spinner.adapter = adapter
                        question2Spinner.adapter = adapter
                    }
                }
            }

            override fun onFailure(call: Call<List<SecurityQuestion>>, t: Throwable) {
                Log.e("DEBUG_API", "PASO 3 (ERROR): ${t.message}")
            }
        })
    }
}