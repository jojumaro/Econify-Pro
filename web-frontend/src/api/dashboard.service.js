import { showToast } from '../utils/ui.utils.js';

let lineChart = null;
let donutChart = null;

// --- 1. FUNCIONES DE RENDERIZADO ---

const renderSummary = (summary) => {
    const expensesEl = document.getElementById('month-expenses');
    const balanceEl = document.getElementById('month-balance');
    const balanceProgress = document.getElementById('balance-progress');

    if (expensesEl) expensesEl.textContent = `€${summary.gastos.toLocaleString('es-ES')}`;
    if (balanceEl) balanceEl.textContent = `€${summary.balance.toLocaleString('es-ES')}`;

    if (balanceProgress) {
        balanceProgress.style.width = `${Math.min(summary.ahorro_porcentaje, 100)}%`;
    }
};

const renderTransactions = (transactions) => {
    const container = document.getElementById('transactions-container');
    if (!container) return;

    if (!transactions || transactions.length === 0) {
        container.innerHTML = '<p style="color: var(--text-secondary); text-align: center; padding: 20px;">No hay movimientos en este periodo.</p>';
        return;
    }

    container.innerHTML = transactions.map(t => {
        const description = t.description || 'Sin descripción';
        const category = t.category_name || 'Sin categoría';
        const amount = t.amount ? parseFloat(t.amount).toLocaleString('es-ES') : '0,00';
        const isIngreso = t.type === 'INGRESO';

        return `
            <div class="transaction-item" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--border-color);">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <div style="padding: 8px; border-radius: 8px; background: ${isIngreso ? '#e8f5e9' : '#ffebee'}">
                        <i data-lucide="${isIngreso ? 'trending-up' : 'trending-down'}"
                           style="width: 16px; color: ${isIngreso ? 'var(--primary-teal)' : 'var(--red)'}"></i>
                    </div>
                    <div>
                        <div style="font-weight: 600; font-size: 14px;">${description}</div>
                        <div style="font-size: 12px; color: var(--text-secondary);">${category}</div>
                    </div>
                </div>
                <div style="font-weight: 700; color: ${isIngreso ? 'var(--primary-teal)' : 'var(--text-main)'}">
                    ${isIngreso ? '+' : '-'}€${amount}
                </div>
            </div>
        `;
    }).join('');

    if (window.lucide) lucide.createIcons();
};

const renderGoals = (goals) => {
    const container = document.getElementById('goals-container');
    if (!container) return;

    // LIMPIEZA: Si no hay metas, mostramos mensaje vacío en lugar de mantener las viejas
    if (!goals || goals.length === 0) {
        container.innerHTML = `
            <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; padding: 40px 0;">
                <i data-lucide="target" style="width: 40px; color: var(--border-color); margin-bottom: 10px;"></i>
                <p style="color: var(--text-secondary); font-size: 13px;">No hay metas activas para este periodo.</p>
            </div>`;
        if (window.lucide) lucide.createIcons();
        return;
    }

    const limitedGoals = goals.slice(0, 3);
    container.innerHTML = limitedGoals.map(g => {
        const percentage = Math.min(Math.round((g.current_amount / g.target_amount) * 100), 100);
        return `
            <div class="goal-item" style="margin-bottom: 25px;">
                <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px;">
                    <span style="font-weight: 600; color: var(--text-main);">${g.name}</span>
                    <span style="font-weight: 700; color: var(--primary-blue);">${percentage}%</span>
                </div>
                <div class="progress-container" style="height: 10px; background: #f1f5f9; border-radius: 6px; overflow: hidden;">
                    <div class="progress-fill" style="width: ${percentage}%; background-color: var(--primary-blue); height: 100%; transition: width 0.8s ease;"></div>
                </div>
            </div>`;
    }).join('');
};

const initCharts = (summary) => {
    // 1. Gráfico de Dona (Derecha)
    const ctxDonutEl = document.getElementById('donutChart');
    if (ctxDonutEl) {
        if (donutChart) donutChart.destroy();

        const hasData = (summary.gastos > 0 || summary.balance > 0);
        donutChart = new Chart(ctxDonutEl.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: hasData ? ['Gastos', 'Balance'] : ['Sin datos'],
                datasets: [{
                    data: hasData ? [summary.gastos, summary.balance] : [1],
                    backgroundColor: hasData ? ['#ef4444', '#22c55e'] : ['#f1f5f9'],
                    borderWidth: 0,
                    cutout: '75%'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } }
            }
        });
        document.getElementById('donut-percentage').textContent = `${Math.round(summary.ahorro_porcentaje)}%`;
    }

    // 2. Gráfico de Línea (Izquierda) - EL QUE SE QUEDA CONGELADO
    const ctxLineEl = document.getElementById('mainDashboardChart');
    if (ctxLineEl) {
        // FORZADO: Si ya existe un gráfico, lo destruimos completamente
        if (lineChart) {
            lineChart.destroy();
            lineChart = null; // Limpiamos la referencia
        }

        const ctx = ctxLineEl.getContext('2d');

        // Si el balance es 0, dibujamos una línea plana en la base
        // Usamos 5 puntos para que la línea cruce todo el gráfico
        const isVacio = (summary.balance === 0 && summary.gastos === 0);
        const dataPuntos = isVacio ? [0, 0, 0, 0, 0] : [summary.balance * 0.7, summary.balance * 0.9, summary.balance * 0.8, summary.balance * 1.1, summary.balance];
        const labelsPuntos = isVacio ? ['-', '-', '-', '-', '-'] : ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4', 'Hoy'];

        const gradient = ctx.createLinearGradient(0, 0, 0, 250);
        gradient.addColorStop(0, isVacio ? 'rgba(203, 213, 225, 0.1)' : 'rgba(37, 99, 235, 0.2)');
        gradient.addColorStop(1, 'rgba(255, 255, 255, 0)');

        lineChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labelsPuntos,
                datasets: [{
                    label: 'Balance (€)',
                    data: dataPuntos,
                    borderColor: isVacio ? '#cbd5e1' : '#2563eb', // Gris si está vacío
                    backgroundColor: gradient,
                    fill: true,
                    tension: 0.4,
                    borderWidth: 3,
                    pointRadius: isVacio ? 0 : 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { color: '#94a3b8' }
                    },
                    y: {
                        grid: { color: '#f1f5f9' },
                        beginAtZero: true,
                        // Forzamos un rango visible si no hay datos
                        suggestedMax: isVacio ? 100 : undefined
                    }
                }
            }
        });
    }
};

// --- 2. GESTIÓN DE PERIODOS Y FILTROS ---

const loadPeriodFilters = async () => {
    const token = localStorage.getItem('access_token');
    try {
        const response = await fetch('/api/dashboard/periods/', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();

        const yearSelector = document.getElementById('year-selector');
        const monthSelector = document.getElementById('month-selector');

        if (yearSelector && monthSelector) {
            yearSelector.innerHTML = data.years.map(y => `<option value="${y}">${y}</option>`).join('');
            monthSelector.innerHTML = data.months.map(m => `<option value="${m.id}">${m.name}</option>`).join('');

            const hoy = new Date();
            yearSelector.value = hoy.getFullYear();
            monthSelector.value = hoy.getMonth() + 1;

            yearSelector.addEventListener('change', () => initDashboard());
            monthSelector.addEventListener('change', () => initDashboard());
        }
    } catch (error) {
        console.error("Error cargando periodos:", error);
    }
};

const initDashboard = async () => {
    const token = localStorage.getItem('access_token');
    if (!token) {
        window.location.href = '/';
        return;
    }

    const m = document.getElementById('month-selector')?.value || (new Date().getMonth() + 1);
    const y = document.getElementById('year-selector')?.value || new Date().getFullYear();

    try {
        const response = await fetch(`/api/dashboard/?month=${m}&year=${y}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            const periodText = document.getElementById('current-period-text');
            if (periodText) periodText.textContent = `Resumen de ${data.summary.periodo_nombre}`;

            renderSummary(data.summary);
            renderTransactions(data.recent_transactions);
            renderGoals(data.goals);
            initCharts(data.summary);
        }
    } catch (error) {
        console.error("Error al cargar dashboard:", error);
    }
};

document.addEventListener('DOMContentLoaded', async () => {
    await loadPeriodFilters();
    initDashboard();
});