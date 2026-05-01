package com.jjmr.econifypro.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
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

        // 1. Referencias de UI
        val etName = view.findViewById<TextInputEditText>(R.id.etProfileName)
        val etLastName = view.findViewById<TextInputEditText>(R.id.etProfileLastName)
        val etNewPass = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPass = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        //val spQuestion1 = view.findViewById<Spinner>(R.id.spProfileQuestion1)
        //val etAnswer1 = view.findViewById<TextInputEditText>(R.id.etProfileAnswer1)
        //val spQuestion2 = view.findViewById<Spinner>(R.id.spProfileQuestion2)
        //val etAnswer2 = view.findViewById<TextInputEditText>(R.id.etProfileAnswer2)
        val avatarCircle = view.findViewById<TextView>(R.id.profileAvatarCircle)
        val btnSave = view.findViewById<Button>(R.id.btnUpdateProfile)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // 2. Configuración de Spinners
        val questions = arrayOf(
            "¿Nombre de tu mascota?",    // ID 1
            "¿Ciudad de nacimiento?",    // ID 2
            "¿Marca de tu primer coche?", // ID 3
            "¿Nombre de tu abuela?"       // ID 4
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, questions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //spQuestion1.adapter = adapter
       // spQuestion2.adapter = adapter

        // 3. Carga de datos desde SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)

        val currentName = sharedPref.getString("user_firstname", "")
        val currentLastName = sharedPref.getString("user_lastname", "")
        val idQ1 = sharedPref.getString("user_q1", "")
        val ans1 = sharedPref.getString("user_a1", "")
        val idQ2 = sharedPref.getString("user_q2", "")
        val ans2 = sharedPref.getString("user_a2", "")

        // 4. Rellenar campos básicos
        etName.setText(currentName)
        etLastName.setText(currentLastName)
        avatarCircle.text = if (!currentName.isNullOrEmpty()) currentName.take(1).uppercase() else "U"

        // 5. Sincronizar Spinners (ID -> Position)
        fun setSpinnerSelection(spinner: Spinner, idStr: String?) {
            idStr?.toIntOrNull()?.let { id ->
                val position = id - 1
                if (position in questions.indices) spinner.setSelection(position)
            }
        }
        /*
        setSpinnerSelection(spQuestion1, idQ1)
        etAnswer1.setText(ans1)
        setSpinnerSelection(spQuestion2, idQ2)
        etAnswer2.setText(ans2)
        */

        // 6. Botón Guardar
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val pass = etNewPass.text.toString()
            val confirm = etConfirmPass.text.toString()

            if (name.isEmpty()) {
                etName.error = "El nombre es obligatorio"
                return@setOnClickListener
            }

            // Estas llaves deben coincidir con lo que espera tu API
            val updateData = mutableMapOf<String, String>().apply {
                put("firstname", name)
                put("lastname", lastName)
                /*
                put("security_question_1", (spQuestion1.selectedItemPosition + 1).toString())
                put("security_answer_1", etAnswer1.text.toString().trim())
                put("security_question_2", (spQuestion2.selectedItemPosition + 1).toString())
                put("security_answer_2", etAnswer2.text.toString().trim())

                 */
            }

            if (pass.isNotEmpty()) {
                if (pass == confirm) {
                    updateData["password"] = pass
                } else {
                    etConfirmPass.error = "Las contraseñas no coinciden"
                    return@setOnClickListener
                }
            }

            val token = sharedPref.getString("access_token", "") ?: ""
            actualizarPerfilRemoto("Bearer $token", updateData)
        }

        // 7. Cerrar Sesión
        btnLogout.setOnClickListener {
            sharedPref.edit().clear().apply()
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun actualizarPerfilRemoto(tokenHeader: String, data: Map<String, String>) {
        val apiService = NetworkConfig.getApiService(requireContext())
        apiService.updateProfile(tokenHeader, data).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val sp = requireActivity().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
                    with(sp.edit()) {
                        putString("user_firstname", data["firstname"])
                        putString("user_lastname", data["lastname"])

                        // CORRECCIÓN AQUÍ: Usamos las llaves correctas del mapa 'data'
                        putString("user_q1", data["security_question_1"])
                        putString("user_a1", data["security_answer_1"])
                        putString("user_q2", data["security_question_2"])
                        putString("user_a2", data["security_answer_2"])
                        apply()
                    }
                    Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Error servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(requireContext(), "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}