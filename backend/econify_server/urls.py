from django.contrib import admin
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from api.views import (
    TransactionViewSet, GoalViewSet, CategoryViewSet, register_user,
    UserProfileView, LoginView, DashboardSummaryView, dashboard_page_view,
    profile_page_view, login_page_view, register_page_view,
    forgot_password_page_view, get_security_questions, get_user_questions,
    verify_identity, reset_password_page_view, reset_password_confirm,
    categories_page_view, transactions_page_view)
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)

# El router gestiona automáticamente las URLs de los ViewSets (CRUD)
router = DefaultRouter()
router.register(r'transactions', TransactionViewSet, basename='transaction')
router.register(r'goals', GoalViewSet, basename='goals')
router.register(r'categories', CategoryViewSet, basename='categories')

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/', include(router.urls)),

    # REGISTRO
    path('api/register/', register_user, name='register'),
    path('register/', register_page_view, name='register_html'),

    # LOGIN
    path('api/token/', LoginView.as_view(), name='token_obtain_pair'),
    path('api/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('', login_page_view, name='login_html'),

    # PERFIL
    path('api/user/profile/', UserProfileView.as_view(), name='user-profile-update'),
    path('profile/', profile_page_view, name='profile_html'),

    # DASHBOARD
    path('api/dashboard/', DashboardSummaryView.as_view(), name='dashboard-summary'),
    path('dashboard/', dashboard_page_view, name='dashboard_html'),

    # CATEGORÍAS
    path('categories/', categories_page_view, name='categories_html'),

    # TRANSACCIONES
    path('transactions/', transactions_page_view, name='transactions_html'),

    # RECUPERAR PASSWORD
    # 1. Url para la vista
    path('forgot-password/', forgot_password_page_view, name='forgot_password_html'),
    # 2. Lista de todas las preguntas (para el registro)
    path('api/security-questions/', get_security_questions, name='security-questions'),
    # 3. Preguntas de un usuario específico (para recuperar contraseña)
    path('api/get-user-questions/', get_user_questions, name='get-user-questions'),
    # 4. Verificar las respuestas (el botón del modal)
    path('api/verify-identity/', verify_identity, name='verify-identity'),

    # RESET PASSWORD
    path('reset-password/', reset_password_page_view, name='reset_password_html'),
    path('api/reset-password-confirm/', reset_password_confirm, name='reset_password_confirm_api'),
]