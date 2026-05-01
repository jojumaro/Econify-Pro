import { showToast } from '../utils/ui.utils.js';

const goalsGrid = document.getElementById('goals-grid');
const goalModal = document.getElementById('goal-modal');
const goalForm = document.getElementById('goal-form');

const goalIdInput = document.getElementById('goal-id');
const goalNameInput = document.getElementById('goal-name');
const goalTargetInput = document.getElementById('goal-target');
const goalCurrentInput = document.getElementById('goal-current');
const goalTypeSelect = document.getElementById('goal-type');

const monthlySettings = document.getElementById('monthly-settings');
const goalYearSelect = document.getElementById('goal-year');
const goalMonthSelect = document.getElementById('goal-month');

const token = localStorage.getItem('access_token');
let allGoals = [];
let currentFilters = {
    status: 'all',
    year: new Date().getFullYear().toString(),
    type: 'all'
};

// --- CONFIGURACIÓN VISUAL POR TIPO ---
const getGoalConfig = (type) => {
    const configs = {
        'SAVING': { icon: 'piggy-bank', color: '#2563eb', label: 'Ahorro' },
        'MONTHLY_SAVING': { icon: 'calendar-check', color: '#0891b2', label: 'Mensual' },
        'ANNUAL_SAVING': { icon: 'calendar-days', color: '#4f46e5', label: 'Anual' },
        'INVESTMENT': { icon: 'trending-up', color: '#10b981', label: 'Inversión' },
        'DEBT_REDUCTION': { icon: 'shield-alert', color: '#ef4444', label: 'Deuda' }
    };
    return configs[type] || configs['SAVING'];
};

const getLabelMonth = (num) => {
    const meses = ["", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
    return meses[num];
};

// --- CARGAR Y RENDERIZAR ---
const fetchGoals = async () => {
    try {
        // Añadimos / al final y el timestamp es VITAL
        const response = await fetch(`/api/goals/?t=${Date.now()}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/json', // Esto le dice a Django: "Ni se te ocurra darme HTML"
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        if (response.ok) {
            const data = await response.json();
            allGoals = data;
            applyFilters();
        }
    } catch (error) {
        console.error("Error en fetch:", error);
    }
};

const renderGoals = (goals) => {
    if (!goalsGrid) return;

    // Fecha de referencia real (Hoy: 28 de Abril de 2026)
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;

    goalsGrid.innerHTML = goals.map(g => {
        const config = getGoalConfig(g.goal_type);
        const amount = parseFloat(g.current_amount) || 0;
        const target = parseFloat(g.target_amount) || 0;

        // Calculamos el porcentaje real (puede ser negativo según tu DB)
        const exactPercentage = target !== 0 ? Math.round((amount / target) * 100) : 0;

        // --- LÓGICA DE ESTADO (Priorizando Deadline) ---
        let isExpired = false;

        if (g.deadline) {
            // Si existe deadline (como tu "2026-04-30"), comparamos fechas completas
            const deadlineDate = new Date(g.deadline);
            // Solo expira si el día de hoy ya es estrictamente posterior al deadline
            if (deadlineDate < now.setHours(0,0,0,0)) {
                isExpired = true;
            }
        } else if (g.year && g.month) {
            // Si no hay deadline, usamos la lógica de mes/año (controlando nulos)
            if (parseInt(g.year) < currentYear || (parseInt(g.year) === currentYear && parseInt(g.month) < currentMonth)) {
                isExpired = true;
            }
        }

        const isDebtPaid = g.goal_type === 'DEBT_REDUCTION' && amount <= 0;
        const isCompleted = isExpired || isDebtPaid || exactPercentage >= 100;
        const isOver = exactPercentage > 100 && !isExpired;

        // Determinamos la clase visual y el texto del badge
        let statusClass = 'status-active';
        let badgeText = config.label;
        let iconName = config.icon;

        if (isExpired) {
            statusClass = 'status-completed expired';
            badgeText = 'PERIODO FINALIZADO';
            iconName = 'lock';
        } else if (isOver) {
            statusClass = 'status-overachieved';
            badgeText = 'META SUPERADA';
            iconName = 'star';
        } else if (isCompleted) {
            statusClass = 'status-completed';
            badgeText = 'META CUMPLIDA';
            iconName = 'check-circle';
        }

        // Formateo de información de periodo (Evita "UNDEFINED NULL")
        const monthName = g.month ? getLabelMonth(g.month) : '';
        const yearLabel = g.year || '';
        const periodInfo = g.goal_type === 'MONTHLY_SAVING' ? ` • ${monthName} ${yearLabel}` : (yearLabel ? ` • ${yearLabel}` : '');

        return `
            <div class="goal-card ${statusClass}">
<div class="goal-header">
    <div class="goal-info-main">
        <div class="goal-badge">
            <i data-lucide="${iconName}" style="width: 14px; height: 14px;"></i>
            <span>${badgeText}${periodInfo}</span>
        </div>
        <h3>${g.name}</h3>
    </div>
    <div class="goal-actions">
        <button class="btn-action edit-btn edit-goal-btn" data-id="${g.id}" title="Editar">
            <i data-lucide="pencil"></i>
        </button>
        <button class="btn-action delete-btn delete-goal-btn" data-id="${g.id}" title="Eliminar">
            <i data-lucide="trash-2"></i>
        </button>
    </div>
</div>

                <div class="goal-amount">
                    ${amount.toLocaleString('es-ES', {minimumFractionDigits: 2})}€
                    <span class="target-label">/ ${target.toLocaleString('es-ES', {minimumFractionDigits: 2})}€</span>
                </div>

                <div class="progress-container">
                    <div class="progress-bar"
                         style="width: ${Math.max(0, Math.min(Math.abs(exactPercentage), 100))}%;
                                background: ${isCompleted ? '#10b981' : config.color};">
                    </div>
                </div>

                <div class="goal-footer">
                    <span style="color: var(--text-secondary);">${isExpired ? 'Resultado Final' : 'Progreso'}</span>
                    <span class="percentage-val" style="color: ${isCompleted ? '#10b981' : config.color}">
                        ${exactPercentage}%
                    </span>
                </div>
            </div>`;
    }).join('');

    // Reinicializar iconos de Lucide y eventos
    lucide.createIcons();
    attachActions();
};

// --- GUARDAR ---
const saveGoal = async (e) => {
    e.preventDefault();
    const id = goalIdInput.value;
    const type = goalTypeSelect.value;
    const year = parseInt(goalYearSelect.value);
    const month = parseInt(goalMonthSelect.value);

    const data = {
        name: goalNameInput.value,
        target_amount: parseFloat(goalTargetInput.value),
        current_amount: parseFloat(goalCurrentInput.value) || 0,
        goal_type: type,
        year: (type === 'MONTHLY_SAVING' || type === 'ANNUAL_SAVING') ? year : null,
        month: type === 'MONTHLY_SAVING' ? month : null,
        is_active: true
    };

    try {
        const response = await fetch(id ? `/api/goals/${id}/` : '/api/goals/', {
            method: id ? 'PUT' : 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            showToast(id ? "Meta actualizada" : "Meta creada", "success");
            closeModal();
            fetchGoals();
        }
    } catch (error) { showToast("Error al guardar", "error"); }
};

// --- MODAL Y EVENTOS ---
const openModal = (data = null) => {
    if (data) {
        document.getElementById('modal-title').innerText = "Editar Meta";
        goalIdInput.value = data.id;
        goalNameInput.value = data.name;
        goalTargetInput.value = data.target_amount;
        goalCurrentInput.value = data.current_amount;
        goalTypeSelect.value = data.goal_type;
        goalTypeSelect.dispatchEvent(new Event('change'));
        if (data.year) goalYearSelect.value = data.year;
        if (data.month) goalMonthSelect.value = data.month;
    } else {
        document.getElementById('modal-title').innerText = "Nueva Meta";
        goalForm.reset();
        goalIdInput.value = "";
        goalMonthSelect.value = new Date().getMonth() + 1;
        monthlySettings.classList.add('hidden');
    }
    goalModal.classList.remove('hidden');
};

const closeModal = () => goalModal.classList.add('hidden');

const attachActions = () => {
    document.querySelectorAll('.delete-goal-btn').forEach(btn => {
        btn.onclick = () => deleteGoal(btn.dataset.id);
    });
    document.querySelectorAll('.edit-goal-btn').forEach(btn => {
        btn.onclick = async () => {
            const res = await fetch(`/api/goals/${btn.dataset.id}/`, { headers: { 'Authorization': `Bearer ${token}` } });
            openModal(await res.json());
        };
    });
};

const deleteGoal = async (id) => {
    if (!confirm("¿Eliminar esta meta?")) return;
    const res = await fetch(`/api/goals/${id}/`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
    if (res.ok) { fetchGoals(); showToast("Meta eliminada"); }
};

document.addEventListener('DOMContentLoaded', () => {
    initFilterEvents();
    fetchGoals();

    const currentYear = new Date().getFullYear();
    for (let i = 0; i <= 2; i++) {
        const opt = document.createElement('option');
        opt.value = currentYear + i; opt.textContent = currentYear + i;
        goalYearSelect.appendChild(opt);
    }

    goalForm.onsubmit = saveGoal;

    goalTypeSelect.addEventListener('change', () => {
        const type = goalTypeSelect.value;
        const isMonthly = type === 'MONTHLY_SAVING';
        const isAnnual = type === 'ANNUAL_SAVING';
        monthlySettings.classList.toggle('hidden', !isMonthly && !isAnnual);
        goalMonthSelect.parentElement.style.display = isMonthly ? 'block' : 'none';
    });

    document.getElementById('btn-open-goal-modal').onclick = () => openModal();
    document.getElementById('btn-cancel').onclick = closeModal;
});

window.markAsCompleted = async (id) => {
    if (!confirm("¿Has alcanzado este objetivo? Se marcará como completada.")) return;

    try {
        const response = await fetch(`/api/goals/${id}/`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status: 'COMPLETED' })
        });

        if (response.ok) {
            showToast("¡Enhorabuena por el objetivo cumplido!", "success");
            fetchGoals(); // Refrescamos la lista
        }
    } catch (error) {
        showToast("No se pudo actualizar la meta", "error");
    }
};

const applyFilters = () => {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;

    let filtered = allGoals.filter(g => {
        const amount = parseFloat(g.current_amount) || 0;
        const target = parseFloat(g.target_amount) || 0;
        const percentage = (amount / target) * 100;

        // Misma lógica de estado que en el render
        const isExpired = (g.year && g.month) && (
            (parseInt(g.year) < currentYear) ||
            (parseInt(g.year) === currentYear && parseInt(g.month) < currentMonth)
)       ;
        const isDebtPaid = g.goal_type === 'DEBT_REDUCTION' && amount <= 0;
        const isCompleted = g.status === 'COMPLETED' || percentage >= 100 || isExpired || isDebtPaid;

        // A. Filtrado por Estado
        if (currentFilters.status === 'active' && isCompleted) return false;
        if (currentFilters.status === 'completed' && !isCompleted) return false;

        // B. Filtrado por Año
        if (currentFilters.year !== 'all') {
            if (g.year && g.year.toString() !== currentFilters.year) return false;
        }

        // C. Filtrado por Tipo
        if (currentFilters.type !== 'all' && g.goal_type !== currentFilters.type) return false;

        return true;
    });

    renderGoals(filtered);
};

const initFilterEvents = () => {
    // Filtros de botones (Todas, Activas, Completadas)
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            currentFilters.status = e.target.dataset.filter;
            applyFilters();
        });
    });

    // Selectores (Año y Tipo)
    document.getElementById('sort-year').addEventListener('change', (e) => {
        currentFilters.year = e.target.value;
        applyFilters();
    });

    document.getElementById('filter-type').addEventListener('change', (e) => {
        currentFilters.type = e.target.value;
        applyFilters();
    });
};