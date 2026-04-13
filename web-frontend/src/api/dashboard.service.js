import { showToast } from '../utils/ui.utils.js';

const initDashboard = async () => {
    const token = localStorage.getItem('access_token');
    if (!token) {
        window.location.href = '/';
        return;
    }

    try {
        const response = await fetch('/api/dashboard/', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            renderSummary(data.summary);
            renderTransactions(data.recent_transactions);
            renderGoals(data.goals);
        } else {
            // Si el token no es válido, mejor limpiar y redirigir
            localStorage.clear();
            window.location.href = '/';
        }
    } catch (error) {
        console.error("Error al cargar dashboard:", error);
        showToast("Error al conectar con el servidor", "error");
    }
};

const renderSummary = (summary) => {
    // Coincidiendo con los IDs de tu HTML
    const expensesEl = document.getElementById('month-expenses');
    const balanceEl = document.getElementById('month-balance');
    const balanceProgress = document.getElementById('balance-progress');

    if (expensesEl) expensesEl.textContent = `€${summary.gastos.toLocaleString('es-ES')}`;
    if (balanceEl) balanceEl.textContent = `€${summary.balance.toLocaleString('es-ES')}`;

    // Actualizamos la barra de progreso del ahorro (si existe el ID)
    if (balanceProgress) {
        balanceProgress.style.width = `${Math.min(summary.ahorro_porcentaje, 100)}%`;
    }
};

const renderTransactions = (transactions) => {
    const container = document.getElementById('transactions-container');
    if (!container) return;

    if (transactions.length === 0) {
        container.innerHTML = '<p style="color: var(--text-secondary); text-align: center; padding: 20px;">No hay movimientos recientes.</p>';
        return;
    }

    container.innerHTML = transactions.map(t => `
        <div class="transaction-item" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--border-color);">
            <div style="display: flex; align-items: center; gap: 12px;">
                <div style="padding: 8px; border-radius: 8px; background: ${t.type === 'INGRESO' ? '#e8f5e9' : '#ffebee'}">
                    <i data-lucide="${t.type === 'INGRESO' ? 'trending-up' : 'trending-down'}"
                       style="width: 16px; color: ${t.type === 'INGRESO' ? 'var(--primary-teal)' : 'var(--red)'}"></i>
                </div>
                <div>
                    <div style="font-weight: 600; font-size: 14px;">${t.description}</div>
                    <div style="font-size: 12px; color: var(--text-secondary);">${t.category_name || 'Sin categoría'}</div>
                </div>
            </div>
            <div style="font-weight: 700; color: ${t.type === 'INGRESO' ? 'var(--primary-teal)' : 'var(--text-main)'}">
                ${t.type === 'INGRESO' ? '+' : '-'}€${parseFloat(t.amount).toLocaleString('es-ES')}
            </div>
        </div>
    `).join('');

    // Importante: Refrescar iconos de Lucide después de inyectar HTML
    if (window.lucide) lucide.createIcons();
};

const renderGoals = (goals) => {
    const container = document.getElementById('goals-container');
    if (!container) return;

    if (goals.length === 0) {
        container.innerHTML = '<p style="color: var(--text-secondary); font-size: 13px;">No tienes metas activas.</p>';
        return;
    }

    container.innerHTML = goals.map(g => {
        const percentage = Math.min(Math.round((g.current_amount / g.target_amount) * 100), 100);
        return `
            <div class="goal-item" style="margin-bottom: 15px;">
                <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 5px;">
                    <span>${g.name}</span>
                    <span style="font-weight: 700;">${percentage}%</span>
                </div>
                <div class="progress-container" style="height: 8px; background: #eee; border-radius: 4px; overflow: hidden;">
                    <div class="progress-fill" style="width: ${percentage}%; background-color: var(--primary-blue); height: 100%;"></div>
                </div>
            </div>
        `;
    }).join('');
};

document.addEventListener('DOMContentLoaded', initDashboard);