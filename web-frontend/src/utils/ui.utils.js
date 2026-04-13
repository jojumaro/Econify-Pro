/**
 * Muestra una notificación personalizada en la pantalla.
 * @param {string} message - El texto a mostrar.
 * @param {string} type - 'error' (por defecto) o 'success'.
 */
export function showToast(message, type = 'error') {
    let alert = document.getElementById('auth-alert');

    if (!alert) {
        alert = document.createElement('div');
        alert.id = 'auth-alert';
        alert.innerHTML = `
            <div style="display: flex; align-items: center; gap: 12px;">
                <i id="alert-icon"></i>
                <span id="alert-message"></span>
            </div>
        `;
        document.body.appendChild(alert);
    }

    const messageSpan = alert.querySelector('#alert-message');
    const icon = alert.querySelector('#alert-icon');

    messageSpan.textContent = message;

    // ESTILOS FORZADOS POR JS (Para asegurar visibilidad)
Object.assign(alert.style, {
    position: 'fixed',
    bottom: '30px',
    right: '30px',
    padding: '16px 24px',
    borderRadius: '12px',
    color: 'white',
    fontWeight: '600',
    zIndex: '999999', // ¡Súbelo al máximo!
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.3)',
    opacity: '1', // Prueba a dejarlo en 1 inicial para descartar fallos de animación
    transform: 'translateY(0)',
    backgroundColor: type === 'success' ? '#10b981' : '#ef4444'
});

if (icon) {
    const iconName = type === 'success' ? 'check-circle' : 'alert-circle';
    icon.innerHTML = ''; // Limpiamos contenido previo
    icon.setAttribute('data-lucide', iconName);

    // Solo intentamos crear el icono si Lucide está disponible
    try {
        if (window.lucide) {
            window.lucide.createIcons({
                attrs: {
                    style: "width: 20px; height: 20px;"
                }
            });
        }
    } catch (err) {
        console.warn("Lucide no pudo cargar el icono, pero el mensaje sigue.");
    }
}

    // Animación de entrada
    setTimeout(() => {
        alert.style.opacity = '1';
        alert.style.transform = 'translateY(0)';
    }, 10);

    // Animación de salida tras 3 segundos
    setTimeout(() => {
        alert.style.opacity = '0';
        alert.style.transform = 'translateY(20px)';
        setTimeout(() => {
            alert.style.display = 'none';
        }, 400);
    }, 3000);
}

/**
*
*/
export const checkPasswordStrength = (password) => {
    let strength = 0;
    if (password.length === 0) return { color: '#e0e0e0', text: '', width: '0%', level: 0 };
    if (password.length < 6) return { color: '#ff4d4d', text: 'Muy corta', width: '20%', level: 1 };

    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    if (strength <= 1) return { color: '#ff4d4d', text: 'Débil', width: '33%', level: 2 };
    if (strength <= 3) return { color: '#ffa500', text: 'Normal', width: '66%', level: 3 };
    return { color: '#2ecc71', text: 'Segura', width: '100%', level: 4 };
};

// Función para actualizar la UI del medidor (para no repetir el DOM manipulation)
export const updateStrengthUI = (password, barElem, textElem) => {
    const status = checkPasswordStrength(password);
    barElem.style.width = status.width;
    barElem.style.backgroundColor = status.color;
    textElem.textContent = status.text;
    textElem.style.color = status.color;
    return status.level;
};