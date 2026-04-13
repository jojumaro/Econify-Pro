import { showToast, updateStrengthUI } from '../utils/ui.utils.js';

document.addEventListener('DOMContentLoaded', () => {
    // Selectores del HTML y Componente
    const profileForm = document.querySelector('.profile-form');
    const passwordInput = document.getElementById('password');
    const strengthBar = document.getElementById('strength-bar');
    const strengthText = document.getElementById('strength-text');
    const submitBtn = document.getElementById('btn-profile-submit');

    // 1. Activar Medidor de Fortaleza (Usando tu función updateStrengthUI)
    if (passwordInput && strengthBar) {
        passwordInput.addEventListener('input', () => {
            updateStrengthUI(passwordInput.value, strengthBar, strengthText);
        });
    }

    // 2. Lógica de Envío
    if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            // Estado de carga en el botón
            const originalBtnContent = submitBtn.innerHTML;
            submitBtn.disabled = true;
            submitBtn.textContent = "Guardando...";

            // Recoger datos (usando los IDs de tu profile.html)
            const firstName = document.getElementById('first_name').value;
            const lastName = document.getElementById('last_name').value;
            const newPassword = passwordInput.value;
            const confirmPassword = document.getElementById('confirm_password')?.value;

            // Validación de coincidencia de contraseña
            if (newPassword && newPassword !== confirmPassword) {
                showToast("Las contraseñas no coinciden", "error");
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalBtnContent;
                return;
            }

            const payload = {
                first_name: firstName,
                last_name: lastName
            };

            if (newPassword.trim() !== "") {
                payload.password = newPassword;
            }

            try {
                const response = await fetch('http://localhost:8000/api/user/profile/', {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('access_token')}`
                    },
                    body: JSON.stringify(payload)
                });

                if (response.ok) {
                    showToast("¡Perfil actualizado con éxito!", "success");

                    // Actualizamos localStorage para que los cambios sean persistentes
                    localStorage.setItem('user_firstname', firstName);
                    localStorage.setItem('user_lastname', lastName);

                    // Actualizar el avatar del header en el momento
                    const avatar = document.getElementById('user-avatar-circle');
                    if (avatar) avatar.textContent = firstName.charAt(0).toUpperCase();

                } else {
                    const errorData = await response.json();
                    showToast(errorData.detail || "Error al actualizar", "error");
                }
            } catch (error) {
                showToast("Error de conexión con el servidor", "error");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalBtnContent;
                if (window.lucide) lucide.createIcons();
            }
        });
    }
});