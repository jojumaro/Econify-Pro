import { showToast } from '../utils/ui.utils.js';

const token = localStorage.getItem('access_token');

// Elementos del DOM
const tableBody = document.getElementById('transactions-body');
const modal = document.getElementById('transaction-modal');
const categorySelect = document.getElementById('t-category');
const filterCategory = document.getElementById('filter-category');
const filterType = document.getElementById('filter-type');

// Inputs del Formulario
const tIdInput = document.getElementById('transaction-id');
const tAmountInput = document.getElementById('t-amount');
const tTypeInput = document.getElementById('t-type');
const tCategoryInput = document.getElementById('t-category');
const tDescInput = document.getElementById('t-description');
const tDateInput = document.getElementById('t-date');

let allTransactions = []; // Cache para filtrar sin volver al servidor

// 1. Cargar Categorías para rellenar los Selects
const loadCategories = async () => {
    try {
        const response = await fetch('/api/categories/', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const categories = await response.json();

        const options = categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

        // Rellenar select del modal y del filtro
        categorySelect.innerHTML = `<option value="">Selecciona categoría</option>` + options;
        filterCategory.innerHTML = `<option value="">Todas las categorías</option>` + options;
    } catch (error) {
        console.error("Error cargando categorías:", error);
    }
};

// 2. Cargar Transacciones del servidor
const fetchTransactions = async () => {
    try {
        const response = await fetch('/api/transactions/', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        allTransactions = await response.json();
        applyFilters(); // Renderiza aplicando los filtros actuales
    } catch (error) {
        showToast("Error al cargar movimientos", "error");
    }
};

// 3. Renderizar la tabla
const renderTransactions = (list) => {
    if (list.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" style="padding: 30px; text-align: center; color: var(--text-secondary);">No hay movimientos.</td></tr>`;
        return;
    }

    tableBody.innerHTML = list.map(t => {
        const safeDesc = (t.description || '').replace(/'/g, "\\'");

        return `
        <tr style="border-bottom: 1px solid #f1f5f9;">
            <td style="padding: 15px; font-size: 14px;">${new Date(t.date).toLocaleDateString()}</td>
            <td style="padding: 15px; font-weight: 500;">${t.description || 'Sin descripción'}</td>
            <td style="padding: 15px;">
                <span class="badge" style="background: rgba(37, 99, 235, 0.1); color: var(--primary-blue); padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600;">
                    ${t.category_name || 'Sin categoría'}
                </span>
            </td>
            <td style="padding: 15px; font-weight: 700; color: ${t.type === 'GASTO' ? '#ef4444' : '#22c55e'};">
                ${t.type === 'GASTO' ? '-' : '+'}${t.amount}€
            </td>
            <td style="padding: 15px; text-align: right; display: flex; justify-content: flex-end; gap: 10px;">
                <button onclick="openEditModal(${t.id}, '${t.amount}', '${t.type}', '${t.category || ''}', '${safeDesc}', '${t.date}')" style="background:none; border:none; color:var(--primary-blue); cursor:pointer; padding: 5px;">
                    <i data-lucide="pencil" style="width: 18px; height: 18px;"></i>
                </button>
                <button onclick="deleteTransaction(${t.id})" style="background:none; border:none; color:#94a3b8; cursor:pointer; padding: 5px;">
                    <i data-lucide="trash-2" style="width: 18px; height: 18px;"></i>
                </button>
            </td>
        </tr>
    `}).join('');
    lucide.createIcons();
};

// 4. Lógica de Filtros
const applyFilters = () => {
    const typeVal = filterType.value;
    const catVal = filterCategory.value;

    const filtered = allTransactions.filter(t => {
        const matchesType = typeVal === "" || t.type === typeVal;
        const matchesCat = catVal === "" || t.category == catVal; // Comparación flexible
        return matchesType && matchesCat;
    });

    renderTransactions(filtered);
};

// 5. Guardar Transacción (POST)
document.getElementById('btn-t-save').addEventListener('click', async () => {
    const payload = {
        amount: tAmountInput.value,
        type: tTypeInput.value,
        category: tCategoryInput.value || null,
        description: tDescInput.value.trim(),
        date: tDateInput.value
    };

    if (!payload.amount || !payload.date) {
        showToast("Monto y fecha son obligatorios", "error");
        return;
    }

    try {
        const response = await fetch('/api/transactions/', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showToast("Movimiento registrado", "success");
            modal.classList.add('hidden');
            resetForm();
            fetchTransactions(); // Recargar lista
        } else {
            const err = await response.json();
            showToast(err.detail || "Error al guardar", "error");
        }
    } catch (error) {
        showToast("Error de conexión", "error");
    }
});

// 6. Borrar Transacción
window.deleteTransaction = async (id) => {
    if (!confirm("¿Eliminar esta transacción definitivamente?")) return;

    try {
        const response = await fetch(`/api/transactions/${id}/`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            showToast("Transacción eliminada");
            fetchTransactions();
        }
    } catch (error) {
        showToast("Error al eliminar", "error");
    }
};

// 7. Utilidades del Modal
const resetForm = () => {
    tAmountInput.value = '';
    tDescInput.value = '';
    tCategoryInput.value = '';
    tDateInput.valueAsDate = new Date();
};

document.getElementById('btn-add-transaction').addEventListener('click', () => {
    resetForm();
    modal.classList.remove('hidden');
});

document.getElementById('btn-t-cancel').addEventListener('click', () => {
    modal.classList.add('hidden');
});

// Eventos de Filtro
filterType.addEventListener('change', applyFilters);
filterCategory.addEventListener('change', applyFilters);

// Inicializar
document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
    fetchTransactions();
});