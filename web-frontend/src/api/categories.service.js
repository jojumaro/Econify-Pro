import { showToast } from '../utils/ui.utils.js';

const token = localStorage.getItem('access_token');
const grid = document.getElementById('categories-grid');
const modal = document.getElementById('category-modal');
const categoryInput = document.getElementById('category-name');
const categoryDescInput = document.getElementById('category-description'); // Asegúrate de tener este ID en el HTML
const categoryIdInput = document.getElementById('category-id');
const modalTitle = document.getElementById('modal-title');

// 1. Cargar categorías al inicio
const fetchCategories = async () => {
    try {
        const response = await fetch('/api/categories/', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        const categories = await response.json();
        renderCategories(categories);
    } catch (error) {
        showToast("Error al cargar categorías", "error");
    }
};

// 2. Pintar las categorías en el Grid
const renderCategories = (categories) => {
    if (categories.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-secondary);">
                <p>No tienes categorías creadas todavía.</p>
            </div>`;
        return;
    }

    grid.innerHTML = categories.map(cat => {
        // Limpiamos la descripción para evitar que comillas rompan el onclick
        const safeDesc = (cat.description || '').replace(/'/g, "\\'");

        return `
        <div class="card" style="display: flex; justify-content: space-between; align-items: center; padding: 15px 20px;">
            <div style="display: flex; align-items: center; gap: 12px;">
                <div style="width: 10px; height: 10px; border-radius: 50%; background: var(--primary-blue);"></div>
                <div>
                    <span style="font-weight: 600; display: block;">${cat.name}</span>
                    <p style="font-size: 12px; color: var(--text-secondary); margin: 0;">${cat.description || 'Sin descripción'}</p>
                </div>
            </div>
            <div style="display: flex; gap: 10px;">
                <button onclick="openEditModal(${cat.id}, '${cat.name}', '${safeDesc}')" class="btn-icon" style="color: var(--primary-blue); background: none; border: none; cursor: pointer; display: flex; align-items: center;">
                    <i data-lucide="pencil" style="width: 18px; height: 18px;"></i>
                </button>
                <button onclick="deleteCategory(${cat.id})" class="btn-icon" style="color: var(--red); background: none; border: none; cursor: pointer; display: flex; align-items: center;">
                    <i data-lucide="trash-2" style="width: 18px; height: 18px;"></i>
                </button>
            </div>
        </div>
        `;
    }).join('');
    lucide.createIcons();
};

// 3. Lógica del Modal

document.getElementById('btn-add-category').addEventListener('click', () => {
    modalTitle.textContent = "Nueva Categoría";
    categoryIdInput.value = '';
    categoryInput.value = '';
    categoryDescInput.value = '';
    modal.classList.remove('hidden');
    categoryInput.focus();
});

window.openEditModal = (id, name, desc) => {
    modalTitle.textContent = "Editar Categoría";
    categoryIdInput.value = id;
    categoryInput.value = name;
    categoryDescInput.value = desc;
    modal.classList.remove('hidden');
    categoryInput.focus();
};

document.getElementById('btn-cancel').addEventListener('click', () => {
    modal.classList.add('hidden');
});

// 4. Guardar Categoría (POST o PUT)
document.getElementById('btn-save-category').addEventListener('click', async () => {
    const name = categoryInput.value.trim();
    const description = categoryDescInput.value.trim();
    const id = categoryIdInput.value;

    if (!name) {
        showToast("El nombre es obligatorio", "error");
        return;
    }

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/categories/${id}/` : '/api/categories/';

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name, description }) // Enviamos descripción al backend
        });

        if (response.ok) {
            showToast(id ? "Categoría actualizada" : "Categoría creada", "success");
            modal.classList.add('hidden');
            fetchCategories();
        } else {
            const data = await response.json();
            showToast(data.name ? data.name[0] : "Error al guardar", "error");
        }
    } catch (error) {
        showToast("Error de conexión", "error");
    }
});

// 5. Borrar Categoría
window.deleteCategory = async (id) => {
    if (!confirm("¿Seguro que quieres borrar esta categoría?")) return;

    try {
        const response = await fetch(`/api/categories/${id}/`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            showToast("Categoría eliminada", "success");
            fetchCategories();
        }
    } catch (error) {
        showToast("Error al eliminar", "error");
    }
};

document.addEventListener('DOMContentLoaded', fetchCategories);