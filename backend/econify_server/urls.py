from django.contrib import admin
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from api.views import (TransactionViewSet, GoalViewSet, CategoryViewSet,
                       register_user, UserProfileView, LoginView)
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)

# El router gestiona automáticamente las URLs de los ViewSets (CRUD)
router = DefaultRouter()
router.register(r'transactions', TransactionViewSet, basename='transactions')
router.register(r'goals', GoalViewSet, basename='goals')
router.register(r'categories', CategoryViewSet, basename='categories')

urlpatterns = [
    path('admin/', admin.site.urls),

    # IMPORTANTE: El router debe ir así para que coincida con Android
    path('api/', include(router.urls)),

    # REGISTRO
    path('api/register/', register_user, name='register'),

    # LOGIN (Usa solo UNA de estas, borra las demás que apunten a lo mismo)
    path('api/token/', LoginView.as_view(), name='token_obtain_pair'),
    path('api/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),

    # PERFIL
    path('api/user/profile/', UserProfileView.as_view(), name='user-profile-update'),
]

"""
urlpatterns = [
    path('admin/', admin.site.urls),

    # Endpoints de tu Lógica de Negocio (CRUD)
    path('api/', include(router.urls)),

    # Endpoint de LOGIN
    path('api/login/', LoginView.as_view(), name='token_obtain_pair'),

    # Endpoint de REGISTRO
    path('api/register/', register_user, name='register'),

    # Endpoints de LOGIN (JWT)
    path('api/token/', LoginView.as_view(), name='token_obtain_pair'),
    #path('api/token/', TokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('api/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('api/user/profile/', UserProfileView.as_view(), name='user-profile-update'),
]
"""