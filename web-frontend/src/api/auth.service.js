// IMPORTANTE: La ruta debe coincidir exactamente con tu estructura de carpetas
import { showToast, logout } from '../utils/ui.utils.js';

/**
 * LÓGICA DE LOGOUT
 * Usamos delegación de eventos para que funcione aunque el DOM no haya terminado de cargar.
 */
document.addEventListener('click', (e) => {
    // Buscamos si el clic fue en el enlace de cerrar sesión
    const logoutTarget = e.target.closest('#logout-link');

    if (logoutTarget) {
        e.preventDefault();
        console.log("Iniciando proceso de logout...");

        if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
            // Usamos la función logout que ya tienes en ui.utils.js
            logout();
        }
    }
});

/**
 * LÓGICA DE LOGIN
 */
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.querySelector('.login-form');

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const btn = document.querySelector('.login-btn');
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;

            btn.textContent = "Entrando...";
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
                    localStorage.setItem('user_email', data.email);

                    showToast("¡Bienvenido!", "success");
                    window.location.href = '/dashboard/';
                } else {
                    showToast(data.error || "Error al entrar", 'error');
                }
            } catch (error) {
                showToast("Error de conexión", 'error');
            } finally {
                btn.textContent = "Entrar";
                btn.disabled = false;
            }
        });
    }
});