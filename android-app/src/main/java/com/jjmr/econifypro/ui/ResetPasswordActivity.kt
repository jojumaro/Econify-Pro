package com.jjmr.econifypro.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.jjmr.econifypro.R
import com.jjmr.econifypro.api.NetworkConfig
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : BaseActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmNewPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var resetProgress: ProgressBar

    // Estas variables guardarán lo que enviamos desde la actividad anterior
    private var uid: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // 1. Recuperar los datos del Intent
        uid = intent.getStringExtra("uid")
        token = intent.getStringExtra("token")

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        resetProgress = findViewById(R.id.resetProgress)
    }

    private fun setupListeners() {
        btnResetPassword.setOnClickListener {
            val pass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmNewPassword.text.toString().trim()

            // Validaciones básicas
            if (pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                etConfirmNewPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            if (pass.length < 6) {
                etNewPassword.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }

            // Si llegamos aquí, disparamos a Django
            cambiarPassword(pass)
        }
    }

    private fun cambiarPassword(nuevaPassword: String) {
        setLoading(true, btnResetPassword, resetProgress)

        // Preparamos el cuerpo para Django (uid, token, password)
        val data = mapOf(
            "uid" to (uid ?: ""),
            "token" to (token ?: ""),
            "password" to nuevaPassword
        )

        NetworkConfig.getApiService(this).resetPasswordConfirm(data).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                setLoading(false, btnResetPassword, resetProgress)

                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "¡Contraseña actualizada! Ya puedes entrar.", Toast.LENGTH_LONG).show()

                    // Al terminar, volvemos al Login para que el usuario entre con su nueva clave
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@ResetPasswordActivity, "El enlace ha caducado o es inválido", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                setLoading(false, btnResetPassword, resetProgress)
                Toast.makeText(this@ResetPasswordActivity, "Error de red: verifica tu servidor", Toast.LENGTH_SHORT).show()
            }
        })
    }
}