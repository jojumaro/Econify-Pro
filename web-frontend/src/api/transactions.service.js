import { showToast } from '../utils/ui.utils.js';

const token = localStorage.getItem('access_token');

// --- ELEMENTOS DEL DOM ---
const tableBody = document.getElementById('transactions-body');
const modal = document.getElementById('transaction-modal');
const categorySelect = document.getElementById('t-category');
const goalSelect = document.getElementById('t-goal');
const filterCategory = document.getElementById('filter-category');
const filterType = document.getElementById('filter-type');
const totalIngresosEl = document.getElementById('total-ingresos');
const totalGastosEl = document.getElementById('total-gastos');
const totalBalanceEl = document.getElementById('total-balance');

// Inputs del Formulario Principal
const tIdInput = document.getElementById('transaction-id');
const tAmountInput = document.getElementById('t-amount');
const tTypeInput = document.getElementById('t-type');
const tCategoryInput = document.getElementById('t-category');
const tGoalInput = document.getElementById('t-goal');
const tDescInput = document.getElementById('t-description');
const tDateInput = document.getElementById('t-date');

// Elementos de Nueva Categoría "On-the-fly"
const btnToggleNewCat = document.getElementById('btn-toggle-new-cat');
const btnCancelNewCat = document.getElementById('btn-cancel-new-cat');
const newCatContainer = document.getElementById('new-category-container');
const newCatInput = document.getElementById('new-category-name');

let allTransactions = [];
let allGoals = [];
let currentSort = 'date_desc';

// --- 1. CARGAR CATEGORÍAS ---
const loadCategories = async () => {
    try {
        const response = await fetch('/api/categories/', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const categories = await response.json();
        const options = categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

        categorySelect.innerHTML = `<option value="">Selecciona categoría</option>` + options;
        filterCategory.innerHTML = `<option value="">Todas las categorías</option>` + options;
    } catch (error) {
        console.error("Error cargando categorías:", error);
    }
};

// --- 2. GESTIÓN DE METAS ---
const loadGoals = async () => {
    try {
        const response = await fetch('/api/goals/', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        allGoals = await response.json();
        filterGoalsByTransactionType();
    } catch (error) {
        console.error("Error cargando metas:", error);
    }
};

const filterGoalsByTransactionType = () => {
    const selectedType = tTypeInput.value; // 'GASTO' o 'INGRESO'
    const descInput = document.getElementById('t-description');

    // UX: Cambiar placeholder dinámicamente
    if (selectedType === 'GASTO') {
        descInput.placeholder = "¿En qué gastaste?";
    } else {
        descInput.placeholder = "¿De dónde viene este dinero?";
    }

    let filtered;
    if (selectedType === 'GASTO') {
        // Para GASTOS: Deudas, Inversiones y Ahorros Acumulados
        filtered = allGoals.filter(g =>
            g.status === 'active' &&
            ['DEBT_REDUCTION', 'INVESTMENT', 'SAVING'].includes(g.goal_type)
        );
    } else {
        // Para INGRESOS: Solo Ahorros Acumulados e Inversiones
        filtered = allGoals.filter(g =>
            g.status === 'active' &&
            ['SAVING', 'INVESTMENT'].includes(g.goal_type)
        );
    }

    const options = filtered.map(g => `<option value="${g.id}">${g.name}</option>`).join('');
    goalSelect.innerHTML = `<option value="">Ninguna meta</option>` + options;
};

// --- 3. CARGAR TRANSACCIONES ---
const fetchTransactions = async () => {
    try {
        const response = await fetch('/api/transactions/', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        allTransactions = await response.json();
        setupYearFilter(allTransactions);
        applyFilters();
    } catch (error) {
        showToast("Error al cargar movimientos", "error");
    }
};

// --- 4. RENDERIZAR LA TABLA ---
const renderTransactions = (list) => {
    if (list.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="empty-message">No hay movimientos en este periodo.</td></tr>`;
        return;
    }

tableBody.innerHTML = list.map(t => {
        const isIngreso = t.type === 'INGRESO';

        return `
            <tr>
                <td>${new Date(t.date).toLocaleDateString()}</td>
                <td>
                    <div class="description-cell">
                        <span class="desc-text">${t.description || 'Sin descripción'}</span>
                        ${t.goal_name ? `<span class="goal-tag"><i data-lucide="target"></i> ${t.goal_name}</span>` : ''}
                    </div>
                </td>
                <td>
                    <span class="badge">${t.category_name || 'Sin categoría'}</span>
                </td>
                <!-- AQUÍ: Asegúrate de usar estas clases -->
                <td class="${isIngreso ? 'amount-ingreso' : 'amount-gasto'}">
                    ${isIngreso ? '+' : '-'}€${parseFloat(t.amount).toLocaleString('es-ES', {minimumFractionDigits: 2})}
                </td>
                <td class="text-right">
                    <div class="action-buttons">
                        <button class="btn-icon" onclick="openEditModal(...)">
                            <i data-lucide="pencil"></i>
                        </button>
                        <button class="btn-icon delete-btn" onclick="deleteTransaction(${t.id})">
                            <i data-lucide="trash-2"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');

    if (window.lucide) lucide.createIcons();
};

// --- 5. LÓGICA DE FILTROS ---
const applyFilters = () => {
    const typeVal = filterType.value;
    const catVal = filterCategory.value;
    const monthVal = filterMonth.value;
    const yearVal = filterYear.value;

    const filtered = allTransactions.filter(t => {
        // 1. Creamos la fecha a partir del campo t.date de cada transacción
        const transactionDate = new Date(t.date);

        // 2. Comprobamos cada condición
        const matchesType = typeVal === "" || t.type === typeVal;
        const matchesCat = catVal === "" || t.category == catVal;

        // Importante: usamos transactionDate aquí
        const matchesMonth = monthVal === "" || transactionDate.getMonth() == monthVal;
        const matchesYear = yearVal === "" || transactionDate.getFullYear() == yearVal;

        // 3. Solo si cumple todas, entra en el array filtrado
        return matchesType && matchesCat && matchesMonth && matchesYear;
    });

    updateSummary(filtered);
    renderTransactions(sortTransactions(filtered));
};

const sortTransactions = (transactions) => {
    return transactions.sort((a, b) => {
        const valA = parseFloat(a.amount);
        const valB = parseFloat(b.amount);

        switch (currentSort) {
            case 'date_desc':
                return new Date(b.date) - new Date(a.date);
            case 'date_asc':
                return new Date(a.date) - new Date(b.date);
            case 'amount_desc':
                return Math.abs(b.amount) - Math.abs(a.amount);
            case 'amount_asc':
                return Math.abs(a.amount) - Math.abs(b.amount);
            default:
                return 0;
        }
    });
};

const filterMonth = document.getElementById('filter-month');
const filterYear = document.getElementById('filter-year');

// Llenar años dinámicamente (desde hace 2 años hasta el actual)
const setupYearFilter = (transactions) => {
    // 1. Extraer años únicos usando un Set
    const years = [...new Set(transactions.map(t => new Date(t.date).getFullYear()))];

    // 2. Ordenar de más reciente a más antiguo
    years.sort((a, b) => b - a);

    // 3. Generar el HTML
    let options = '<option value="">Todos los años</option>';
    years.forEach(year => {
        options += `<option value="${year}">${year}</option>`;
    });

    filterYear.innerHTML = options;
};

// --- 6. UI NUEVA CATEGORÍA ---
btnToggleNewCat.addEventListener('click', () => {
    tCategoryInput.classList.add('hidden');
    newCatContainer.classList.remove('hidden');
    btnToggleNewCat.classList.add('hidden');
    newCatInput.focus();
});

btnCancelNewCat.addEventListener('click', () => {
    tCategoryInput.classList.remove('hidden');
    newCatContainer.classList.add('hidden');
    btnToggleNewCat.classList.remove('hidden');
    newCatInput.value = '';
});

// --- 7. GUARDAR ---
document.getElementById('btn-t-save').addEventListener('click', async () => {
    let categoryId = tCategoryInput.value;
    const newCatName = newCatInput.value.trim();

    if (!newCatContainer.classList.contains('hidden') && newCatName) {
        try {
            const catResponse = await fetch('/api/categories/', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: newCatName })
            });
            if (catResponse.ok) {
                const newCat = await catResponse.json();
                categoryId = newCat.id;
                await loadCategories();
            }
        } catch (error) {
            showToast("Error creando categoría", "error");
            return;
        }
    }

    const id = tIdInput.value;
    const payload = {
        amount: tAmountInput.value,
        type: tTypeInput.value,
        category: categoryId || null,
        goal: tGoalInput.value || null,
        description: tDescInput.value.trim(),
        date: tDateInput.value
    };

    if (!payload.amount || !payload.date) {
        showToast("Monto y fecha son obligatorios", "error");
        return;
    }

    const url = id ? `/api/transactions/${id}/` : '/api/transactions/';
    const method = id ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showToast(id ? "Actualizado" : "Registrado", "success");
            modal.classList.add('hidden');
            resetForm();
            fetchTransactions();
        }
    } catch (error) {
        showToast("Error al guardar", "error");
    }
});

// --- 8. ACCIONES GLOBALES ---
window.openEditModal = (id, amount, type, category, desc, date, goalId) => {
    resetForm();
    document.getElementById('transaction-modal-title').innerText = "Editar Movimiento";

    tIdInput.value = id;
    tAmountInput.value = amount;
    tTypeInput.value = type;

    // IMPORTANTE: Refrescar metas antes de asignar el valor seleccionado
    filterGoalsByTransactionType();

    tCategoryInput.value = category;
    tGoalInput.value = goalId || '';
    tDescInput.value = desc;
    tDateInput.value = date;
    modal.classList.remove('hidden');
};

window.deleteTransaction = async (id) => {
    if (!confirm("¿Eliminar movimiento?")) return;
    try {
        const response = await fetch(`/api/transactions/${id}/`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
            showToast("Eliminado");
            fetchTransactions();
        }
    } catch (error) {
        showToast("Error al eliminar", "error");
    }
};

function updateSummary(transactions) {
    let ingresos = 0;
    let gastos = 0;

    transactions.forEach(t => {
        const amount = parseFloat(t.amount) || 0;

        if (t.type === 'INGRESO') {
            ingresos += amount;
        } else if (t.type === 'GASTO') {
            gastos += amount;
        }
    });

    const balance = ingresos - gastos;

    totalIngresosEl.textContent = `+€${ingresos.toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;
    totalGastosEl.textContent = `-€${gastos.toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;
    totalBalanceEl.textContent = `€${balance.toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;
    totalBalanceEl.className = `stat-value ${balance < 0 ? 'text-danger' : 'text-success'}`;
}

// --- 9. INICIALIZACIÓN ---
const resetForm = () => {
    document.getElementById('transaction-modal-title').innerText = "Registrar Movimiento";
    tIdInput.value = '';
    tAmountInput.value = '';
    tDescInput.value = '';
    tCategoryInput.value = '';
    tGoalInput.value = '';
    tDateInput.valueAsDate = new Date();

    tCategoryInput.classList.remove('hidden');
    newCatContainer.classList.add('hidden');
    btnToggleNewCat.classList.remove('hidden');
    newCatInput.value = '';

    tTypeInput.value = 'GASTO';
    filterGoalsByTransactionType();
};

document.getElementById('btn-add-transaction').addEventListener('click', () => {
    resetForm();
    modal.classList.remove('hidden');
});

document.getElementById('btn-t-cancel').addEventListener('click', () => {
    modal.classList.add('hidden');
});

filterType.addEventListener('change', applyFilters);
filterCategory.addEventListener('change', applyFilters);
filterMonth.addEventListener('change', applyFilters);
filterYear.addEventListener('change', applyFilters);
tTypeInput.addEventListener('change', filterGoalsByTransactionType);

document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
    loadGoals();
    fetchTransactions();
    setupYearFilter();

    const sortSelector = document.getElementById('sort-transactions');
    if (sortSelector) {
        sortSelector.addEventListener('change', (e) => {
            currentSort = e.target.value;
            applyFilters();
        });
    }
});

