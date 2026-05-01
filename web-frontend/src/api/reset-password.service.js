import { showToast, updateStrengthUI } from '../utils/ui.utils.js';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('reset-password-form');

    // IDs CORREGIDOS (Los que vienen en el componente _password_section.html)
    const passwordInput = document.getElementById('password');
    const confirmInput = document.getElementById('confirm_password');
    const strengthBar = document.getElementById('strength-bar');
    const strengthText = document.getElementById('strength-text');

    const urlParams = new URLSearchParams(window.location.search);
    const uid = urlParams.get('uid');
    const token = urlParams.get('token');

    // Validación de seguridad por si alguien entra a la URL a mano
    if (!uid || !token) {
        showToast("Enlace de recuperación inválido o expirado", "error");
        setTimeout(() => window.location.href = '/', 2000);
        return;
    }

    // Escuchamos el input para que la barra de fortaleza se mueva
    if (passwordInput && strengthBar) {
        passwordInput.addEventListener('input', () => {
            updateStrengthUI(passwordInput.value, strengthBar, strengthText);
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const password = passwordInput.value;
        const confirmPassword = confirmInput.value;

        if (password !== confirmPassword) {
            showToast("Las contraseñas no coinciden", "error");
            return;
        }

        const btn = form.querySelector('button');
        btn.disabled = true;
        btn.textContent = "Actualizando...";

        try {
            // Usamos ruta relativa /api/ para evitar problemas de CORS
            const response = await fetch('/api/reset-password-confirm/', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ uid, token, password })
            });

            if (response.ok) {
                showToast("¡Contraseña actualizada con éxito!", "success");
                setTimeout(() => window.location.href = '/', 2000);
            } else {
                const data = await response.json();
                showToast(data.error || "El enlace ha expirado. Solicita uno nuevo.", "error");
            }
        } catch (error) {
            showToast("Error de conexión con el servidor", "error");
        } finally {
            btn.disabled = false;
            btn.textContent = "Actualizar contraseña";
        }
    });
});