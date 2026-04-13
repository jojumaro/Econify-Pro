import { showToast } from '../utils/ui.utils.js';

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.querySelector('.login-form');

    // IMPORTANTE: Solo ejecutamos la lógica si el formulario existe en esta página
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const btn = document.querySelector('.login-btn');

            btn.textContent = "Cargando...";
            btn.disabled = true;

            try {
                const response = await fetch('http://localhost:8000/api/token/', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const data = await response.json();

                if (response.ok) {
                    localStorage.setItem('access_token', data.access);
                    localStorage.setItem('refresh_token', data.refresh);
                    localStorage.setItem('user_firstname', data.firstname);
                    localStorage.setItem('user_lastname', data.lastname || data.last_name || "");
                    localStorage.setItem('user_email', data.email);

                    showToast("¡Bienvenido!", "success");

                    window.location.href = '/dashboard/';
                } else {
                    showToast(data.error || "Credenciales incorrectas", 'error');
                }
            } catch (error) {
                console.error("Error de red:", error);
                showToast("No se pudo conectar con el servidor", 'error');
            } finally {
                btn.textContent = "Entrar";
                btn.disabled = false;
            }
        });
    }
});