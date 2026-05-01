package com.jjmr.econifypro.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.jjmr.econifypro.R
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.model.UserQuestionsResponse
import com.jjmr.econifypro.model.VerifyResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var emailInput: EditText
    private lateinit var btnGetQuestions: Button
    private lateinit var layoutStepEmail: LinearLayout

    private lateinit var layoutStepQuestions: LinearLayout
    private lateinit var tvQuestion1: TextView
    private lateinit var tvQuestion2: TextView
    private lateinit var answer1Input: EditText
    private lateinit var answer2Input: EditText
    private lateinit var btnVerifyAnswers: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvBackToLogin: TextView
    private lateinit var tbBackToLoginTextVerify: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        // Paso 1
        emailInput = findViewById(R.id.etForgotEmail)
        btnGetQuestions = findViewById(R.id.btnGetQuestions)
        layoutStepEmail = findViewById(R.id.layoutStepEmail)

        // Paso 2
        layoutStepQuestions = findViewById(R.id.layoutStepQuestions)
        tvQuestion1 = findViewById(R.id.tvQuestion1)
        tvQuestion2 = findViewById(R.id.tvQuestion2)
        answer1Input = findViewById(R.id.etAnswer1)
        answer2Input = findViewById(R.id.etAnswer2)
        btnVerifyAnswers = findViewById(R.id.btnVerifyAnswers)
        progress = findViewById(R.id.forgotProgress)
        tvBackToLogin = findViewById(R.id.backToLoginText)
        tbBackToLoginTextVerify = findViewById(R.id.backToLoginTextVerify)
    }

    private fun setupListeners() {
        tvBackToLogin.setOnClickListener {
            finish()
        }

        tbBackToLoginTextVerify.setOnClickListener {
            finish()
        }

        btnGetQuestions.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                emailInput.error = "Introduce tu email"
                return@setOnClickListener
            }

            setLoading(true, btnGetQuestions, progress)

            NetworkConfig.getApiService(this).getUserQuestions(email).enqueue(object : Callback<UserQuestionsResponse> {
                override fun onResponse(call: Call<UserQuestionsResponse>, response: Response<UserQuestionsResponse>) {
                    setLoading(false, btnGetQuestions, progress)
                    if (response.isSuccessful && response.body() != null) {
                        // Pintamos los textos que Django ya tradujo de IDs a frases
                        tvQuestion1.text = response.body()!!.question1
                        tvQuestion2.text = response.body()!!.question2

                        layoutStepEmail.visibility = View.GONE
                        layoutStepQuestions.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Email no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UserQuestionsResponse>, t: Throwable) {
                    setLoading(false, btnGetQuestions, progress)
                    Toast.makeText(this@ForgotPasswordActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnVerifyAnswers.setOnClickListener {
            val ans1 = answer1Input.text.toString().trim()
            val ans2 = answer2Input.text.toString().trim()
            val email = emailInput.text.toString().trim()

            if (ans1.isEmpty() || ans2.isEmpty()) {
                Toast.makeText(this, "Responde a las preguntas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true, btnVerifyAnswers, progress)

            // Enviamos las respuestas al endpoint 'verify-identity'
            val body = mapOf("email" to email, "ans1" to ans1, "ans2" to ans2)

            NetworkConfig.getApiService(this).verifyIdentity(body).enqueue(object : Callback<VerifyResponse> {
                override fun onResponse(call: Call<VerifyResponse>, response: Response<VerifyResponse>) {
                    // 1. Evitamos fugas de memoria y errores de contexto
                    if (isFinishing || isDestroyed) return

                    setLoading(false, btnVerifyAnswers, progress)

                    if (response.isSuccessful) {
                        val verifyData = response.body()
                        if (verifyData != null) {
                            // 2. Preparamos el Intent con Flags de limpieza
                            val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java).apply {
                                putExtra("uid", verifyData.uid)
                                putExtra("token", verifyData.token)
                                // Evita comportamientos erráticos en la pila de actividades
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }

                            startActivity(intent)

                            // 3. Solo cerramos esta pantalla SI el Intent se lanzó con éxito
                            finish()
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, "Error: El servidor respondió vacío", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Aquí capturamos el error real de Django (400, 404, etc.)
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        android.util.Log.e("API_ERROR", errorMsg)
                        Toast.makeText(this@ForgotPasswordActivity, "Validación fallida: Respuestas incorrectas", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<VerifyResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    setLoading(false, btnVerifyAnswers, progress)
                    Toast.makeText(this@ForgotPasswordActivity, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}