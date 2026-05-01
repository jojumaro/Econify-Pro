from django.shortcuts import render
from django.contrib.auth.decorators import login_required

# --- VISTAS PÚBLICAS (No requieren login) ---

def login_page_view(request):
    """Sirve la página de inicio de sesión."""
    return render(request, 'auth/login.html')

def register_page_view(request):
    """Sirve la página de registro."""
    return render(request, 'auth/register.html')

def forgot_password_page_view(request):
    """Sirve la página de recuperación de contraseña."""
    return render(request, 'auth/forgot-password.html')

def reset_password_page_view(request):
    """Sirve la página para establecer la nueva contraseña."""
    return render(request, 'auth/reset-password.html')


# --- VISTAS PRIVADAS (Requieren estar logueado) ---
# Nota: Si usas JWT 100% en el frontend, esto es opcional,
# pero protege contra accesos accidentales al HTML.

@login_required(login_url='/login-web/') # Redirige si no hay sesión
def dashboard_page_view(request):
    return render(request, 'dashboard.html')

@login_required(login_url='/login-web/')
def profile_page_view(request):
    return render(request, 'auth/profile.html')

@login_required(login_url='/login-web/')
def categories_page_view(request):
    return render(request, 'categories.html')

@login_required(login_url='/login-web/')
def transactions_page_view(request):
    return render(request, 'transactions.html')

@login_required(login_url='/login-web/')
def goals_page_view(request):
    return render(request, 'goals.html')