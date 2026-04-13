import { showToast } from '../utils/ui.utils.js';

const init = () => {
    const form = document.querySelector('.forgot-password-form');
    const modal = document.getElementById('security-modal');
    const verifyBtn = document.getElementById('verify-btn');
    const closeBtn = document.getElementById('close-modal');

    if (!form || !modal) return;

    // A. Pedir preguntas al servidor
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const btn = form.querySelector('button[type="submit"]');

        btn.textContent = "Buscando...";
        btn.disabled = true;

        try {
            // Usamos ruta relativa para que funcione en cualquier entorno
            const response = await fetch(`/api/get-user-questions/?email=${email}`);
            const data = await response.json();

            if (response.ok) {
                // 1. Inyectamos las preguntas que vienen del servidor
                document.getElementById('label-q1').textContent = data.question1;
                document.getElementById('label-q2').textContent = data.question2;

                // 2. Limpiamos respuestas de intentos previos
                document.getElementById('ans1').value = "";
                document.getElementById('ans2').value = "";

                // 3. Mostramos el modal
                modal.style.display = 'flex';
                modal.classList.remove('hidden');

                if(window.lucide) lucide.createIcons();
            } else {
                showToast(data.error || "Email no encontrado", "error");
            }
        } catch (error) {
            showToast("Error de conexión", "error");
        } finally {
            btn.textContent = "Verificar identidad";
            btn.disabled = false;
        }
    });

    // B. Verificar respuestas y redirigir
    verifyBtn.addEventListener('click', async () => {
        const email = document.getElementById('email').value;
        const ans1 = document.getElementById('ans1').value.trim();
        const ans2 = document.getElementById('ans2').value.trim();

        if (!ans1 || !ans2) {
            showToast("Por favor, responde a ambas preguntas", "error");
            return;
        }

        verifyBtn.disabled = true;
        verifyBtn.textContent = "Verificando...";

        try {
            const response = await fetch('/api/verify-identity/', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, ans1, ans2 })
            });

            const data = await response.json();

            if (response.ok) {
                showToast("Identidad confirmada. Redirigiendo...", "success");

                // REDIRECCIÓN A RUTA DE DJANGO (Sin .html)
                setTimeout(() => {
                    window.location.href = `/reset-password/?uid=${data.uid}&token=${data.token}`;
                }, 1500);
            } else {
                showToast(data.error || "Las respuestas no coinciden", "error");
            }
        } catch (error) {
            showToast("Error en el servidor", "error");
        } finally {
            verifyBtn.disabled = false;
            verifyBtn.textContent = "Verificar Identidad";
        }
    });

    closeBtn.addEventListener('click', () => {
        modal.style.display = 'none';
        modal.classList.add('hidden');
    });
};

document.addEventListener('DOMContentLoaded', init);