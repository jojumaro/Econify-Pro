package com.jjmr.econifypro.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.jjmr.econifypro.R
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.jjmr.econifypro.api.NetworkConfig

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Referencias a los componentes del XML
        val etName = view.findViewById<TextInputEditText>(R.id.etProfileName)
        val etLastName = view.findViewById<TextInputEditText>(R.id.etProfileLastName)
        val etNewPass = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPass = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val avatarCircle = view.findViewById<TextView>(R.id.profileAvatarCircle)
        val btnSave = view.findViewById<Button>(R.id.btnUpdateProfile)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // 2. Recuperamos los datos guardados en el Login (EconifyPrefs)
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
        val currentName = sharedPref.getString("user_firstname", null)
        val currentLastName = sharedPref.getString("user_lastname", null)

        // 3. Rellenamos la vista con los datos actuales
        if (currentName != null) {
            etName.setText(currentName)
            // Actualizamos el círculo con la inicial real: "J" de Jorge
            avatarCircle.text = currentName.take(1).uppercase()
        } else {
            // Si sale esto, es que el LoginActivity no guardó la clave "user_firstname"
            etName.setText("Usuario")
            avatarCircle.text = "U"
        }

        etLastName.setText(currentLastName ?: "")

        // 4. Lógica del botón Guardar
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val pass = etNewPass.text.toString()
            val confirm = etConfirmPass.text.toString()

            // Validaciones locales
            if (name.isEmpty()) {
                etName.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }

            val updateData = mutableMapOf<String, String>()
            updateData["firstname"] = name
            updateData["lastname"] = lastName

            if (pass.isNotEmpty()) {
                if (pass != confirm) {
                    etConfirmPass.error = "Las contraseñas no coinciden"
                    return@setOnClickListener
                }
                updateData["password"] = pass
            }

            // Llamada a la API
            val token = sharedPref.getString("access_token", "") ?: ""
            actualizarPerfilRemoto("Bearer $token", updateData)
        }

        // 5. Lógica de Cerrar Sesión (Muy importante para la entrega del proyecto)
        btnLogout.setOnClickListener {
            // Limpiamos las preferencias (Token, nombre, etc.)
            sharedPref.edit().clear().apply()

            // Redirigimos al Login y cerramos esta actividad
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun actualizarPerfilRemoto(token: String, data: Map<String, String>) {
        // 1. Llamamos a getApiService pasándole el contexto del Fragmento
        val apiService = NetworkConfig.getApiService(requireContext())

        // 2. CORRECCIÓN: Usamos 'data' (que es el parámetro) y enviamos SOLO el mapa
        // porque tu AuthInterceptor ya añade el Token automáticamente.
        apiService.updateProfile(data).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    // Actualizamos los datos locales en SharedPreferences
                    val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        // Usamos las claves que vienen en el mapa 'data'
                        putString("user_firstname", data["first_name"])
                        putString("user_lastname", data["last_name"])
                        apply()
                    }

                    Toast.makeText(requireContext(), "¡Perfil actualizado correctamente!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Error al guardar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(requireContext(), "Fallo de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}