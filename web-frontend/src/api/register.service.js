import { showToast, updateStrengthUI } from '../utils/ui.utils.js';

async function loadQuestions() {
    try {
        const response = await fetch('http://localhost:8000/api/security-questions/');
        const questions = response.ok ? await response.json() : [];

        const selects = document.querySelectorAll('.question-select');
        selects.forEach(select => {
            select.innerHTML = '<option value="" disabled selected>Selecciona una pregunta...</option>';

            questions.forEach(q => {
                const option = document.createElement('option');
                // CAMBIO AQUÍ:
                // q ahora es {id: 1, question_text: "..."}
                option.value = q.id;             // Guardamos el ID como valor (más limpio para la DB)
                option.textContent = q.question_text; // Mostramos el texto al usuario
                select.appendChild(option);
            });
        });
    } catch (e) {
        console.error("Error cargando preguntas", e);
        document.querySelectorAll('.question-select').forEach(s => s.innerHTML = '<option disabled>Error al cargar preguntas</option>');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const registerForm = document.querySelector('.register-form');
    const passwordInput = document.getElementById('password');
    const strengthBar = document.getElementById('strength-bar');
    const strengthText = document.getElementById('strength-text');

    // Solo cargamos la lógica si estamos en la página de registro
    if (registerForm) {
        loadQuestions();

        if (passwordInput && strengthBar) {
            passwordInput.addEventListener('input', () => {
                updateStrengthUI(passwordInput.value, strengthBar, strengthText);
            });
        }

        // Validación visual en tiempo real
        const confirmInput = document.getElementById('confirm_password');

        confirmInput.addEventListener('input', () => {
        if (confirmInput.value === passwordInput.value && confirmInput.value !== "") {
            confirmInput.style.borderColor = "#22c55e"; // Verde si coinciden
        } else {
            confirmInput.style.borderColor = "var(--border-color)"; // Normal
        }
});

        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            // 1. Obtener los valores de las contraseñas
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirm_password').value;

            // 2. VALIDACIÓN DE COINCIDENCIA
            if (password !== confirmPassword) {
                showToast("Las contraseñas no coinciden. Por favor, verifícalas.", "error");

                // Opcional: marcar el input en rojo
                document.getElementById('confirm_password').style.borderColor = "#ef4444";
                return; // Detiene la ejecución aquí, no se envía nada al servidor
            }

            const formData = {
                first_name: document.getElementById('first_name').value,
                last_name: document.getElementById('last_name').value,
                email: document.getElementById('email').value,
                password: password,
                security_question_1: document.getElementById('q1').value,
                security_answer_1: document.getElementById('a1').value.trim().toLowerCase(),
                security_question_2: document.getElementById('q2').value,
                security_answer_2: document.getElementById('a2').value.trim().toLowerCase()
            };

            // Seleccionamos el botón de forma segura
            const btn = registerForm.querySelector('button[type="submit"]') || registerForm.querySelector('.login-btn');
            const originalText = btn.textContent;

            btn.textContent = "Creando cuenta...";
            btn.disabled = true;

            try {
                const response = await fetch('http://localhost:8000/api/register/', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });

                const data = await response.json();

                if (response.ok) {
                    localStorage.setItem('user_firstname', formData.firstname);
                    showToast("¡Cuenta creada! Redirigiendo...", "success");
                    setTimeout(() => window.location.href = '/', 2000);
                } else {
                    showToast(data.email ? "Este email ya está en uso" : "Error en el registro", 'error');
                }
            } catch (error) {
                console.error("Error en registro:", error);
                showToast("Error de conexión", 'error');
            } finally {
                btn.textContent = originalText;
                btn.disabled = false;
            }
        });
    }
});