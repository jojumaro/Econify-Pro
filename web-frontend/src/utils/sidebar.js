export function renderSidebar(activePage) {
    const sidebarContainer = document.querySelector('.sidebar');
    if (!sidebarContainer) return;

    // Usamos exactamente el mismo HTML que tienes en el dashboard que sí se ve bien
    sidebarContainer.innerHTML = `
        <div class="login-logo" style="justify-content: flex-start; margin-bottom: 40px;">
            <div class="logo-icon">
                <i data-lucide="zap"></i>
            </div>
            <span class="logo-text">Econify Pro</span>
        </div>

        <nav class="nav-menu" style="display: flex; flex-direction: column; gap: 15px;">
            <a href="../../dashboard.html" class="nav-item ${activePage === 'dashboard' ? 'active' : ''}"
               style="text-decoration: none; display: flex; align-items: center; gap: 10px; color: ${activePage === 'dashboard' ? 'var(--primary-blue)' : 'var(--text-secondary)'}">
                <i data-lucide="layout-dashboard"></i>
                <span>Dashboard</span>
            </a>
            <a href="#" class="nav-item ${activePage === 'transactions' ? 'active' : ''}"
               style="text-decoration: none; display: flex; align-items: center; gap: 10px; color: var(--text-secondary);">
                <i data-lucide="arrow-right-left"></i>
                <span>Transacciones</span>
            </a>
            <a href="#" class="nav-item ${activePage === 'goals' ? 'active' : ''}"
               style="text-decoration: none; display: flex; align-items: center; gap: 10px; color: var(--text-secondary);">
                <i data-lucide="target"></i>
                <span>Metas</span>
            </a>
            <a href="./profile.html" class="nav-item ${activePage === 'profile' ? 'active' : ''}"
               style="text-decoration: none; display: flex; align-items: center; gap: 10px; color: ${activePage === 'profile' ? 'var(--primary-blue)' : 'var(--text-secondary)'}">
                <i data-lucide="user"></i>
                <span>Perfil</span>
            </a>

            <a href="#" id="sidebar-logout" class="nav-item"
               style="text-decoration: none; color: var(--red); display: flex; align-items: center; gap: 10px; margin-top: 20px;">
                <i data-lucide="log-out"></i>
                <span>Cerrar Sesión</span>
            </a>
        </nav>
    `;

    if (window.lucide) window.lucide.createIcons();

    document.getElementById('sidebar-logout')?.addEventListener('click', (e) => {
        e.preventDefault();
        localStorage.clear();
        window.location.href = './login.html';
    });
}