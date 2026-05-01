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
            <div class="empty-state">
                <p>No tienes categorías creadas todavía.</p>
            </div>`;
        return;
    }

    grid.innerHTML = categories.map(cat => {
        const safeDesc = (cat.description || '').replace(/'/g, "\\'");

        return `
        <div class="category-card">
            <div class="category-info">
                <div class="category-dot"></div>
                <div class="category-text">
                    <span class="category-name">${cat.name}</span>
                    <p class="category-desc">${cat.description || 'Sin descripción'}</p>
                </div>
            </div>
            <div class="category-actions">
                <button onclick="openEditModal(${cat.id}, '${cat.name}', '${safeDesc}')" class="btn-icon edit">
                    <i data-lucide="pencil"></i>
                </button>
                <button onclick="deleteCategory(${cat.id})" class="btn-icon delete">
                    <i data-lucide="trash-2"></i>
                </button>
            </div>
        </div>
        `;
    }).join('');

    if (window.lucide) lucide.createIcons();
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