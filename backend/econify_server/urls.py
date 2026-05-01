from django.contrib import admin
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from rest_framework_simplejwt.views import TokenRefreshView

# --- IMPORTS DE LAS NUEVAS VISTAS MODULARES ---
from api.views.transaction_views import TransactionViewSet, transactions_page_view
from api.views.goal_views import GoalViewSet, goals_page_view
from api.views.category_views import CategoryViewSet, categories_page_view
from api.views.auth_views import (
    register_user, UserProfileView, LoginView,
    get_security_questions, get_user_questions,
    verify_identity, reset_password_confirm,
    login_page_view, register_page_view, profile_page_view,
    forgot_password_page_view, reset_password_page_view
)
from api.views.dashboard_views import DashboardSummaryView, dashboard_page_view, DashboardPeriodsView

# El router gestiona automáticamente las URLs de los ViewSets (CRUD)
router = DefaultRouter()
router.register(r'transactions', TransactionViewSet, basename='transaction')
router.register(r'goals', GoalViewSet, basename='goals')
router.register(r'categories', CategoryViewSet, basename='categories')

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/', include(router.urls)),

    # AUTH / REGISTRO
    path('api/register/', register_user, name='register'),
    path('register/', register_page_view, name='register_html'),

    # LOGIN / TOKENS
    path('api/token/', LoginView.as_view(), name='token_obtain_pair'),
    path('api/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('', login_page_view, name='login_html'),

    # PERFIL
    path('api/user/profile/', UserProfileView.as_view(), name='user-profile-update'),
    path('profile/', profile_page_view, name='profile_html'),

    # DASHBOARD
    path('api/dashboard/', DashboardSummaryView.as_view(), name='dashboard-summary'),
    path('api/dashboard/periods/', DashboardPeriodsView.as_view(), name='dashboard-periods'),
    path('dashboard/', dashboard_page_view, name='dashboard_html'),

    # CATEGORÍAS HTML
    path('categories/', categories_page_view, name='categories_html'),

    # TRANSACCIONES HTML
    path('transactions/', transactions_page_view, name='transactions_html'),

    # METAS HTML
    path('goals/', goals_page_view, name='goals_html'),

    # RECUPERAR PASSWORD (LÓGICA API)
    path('api/security-questions/', get_security_questions, name='security-questions'),
    path('api/get-user-questions/', get_user_questions, name='get-user-questions'),
    path('api/verify-identity/', verify_identity, name='verify-identity'),
    path('api/reset-password-confirm/', reset_password_confirm, name='reset_password_confirm_api'),

    # PÁGINAS HTML DE PASSWORD
    path('forgot-password/', forgot_password_page_view, name='forgot_password_html'),
    path('reset-password/', reset_password_page_view, name='reset_password_html'),
]