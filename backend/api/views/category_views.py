from rest_framework import viewsets, permissions
# Importamos desde el nivel superior
from ..models import Category
from ..serializers.category_serializers import CategorySerializer
from django.shortcuts import render
from django.contrib.auth.decorators import login_required

class CategoryViewSet(viewsets.ModelViewSet):
    """
    Gestión de categorías personalizadas por usuario.
    Cada usuario solo puede ver y crear sus propias categorías.
    """
    serializer_class = CategorySerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # Filtramos para que cada usuario solo vea sus categorías
        # El middleware de JWT ya se encarga de que request.user sea el correcto
        return Category.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        # Principio de Integridad: Asignamos el usuario propietario automáticamente
        serializer.save(user=self.request.user)

#@login_required
def categories_page_view(request):
    """Vista para renderizar la página HTML de categorías."""
    return render(request, 'categories.html')