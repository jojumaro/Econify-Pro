import calendar
from datetime import date
from django.db.models import Sum
from django.utils import timezone
from rest_framework import viewsets
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import render
from django.contrib.auth.decorators import login_required

# Imports relativos para la nueva estructura de carpetas
from ..models import Goals, Transaction
from ..serializers.goal_serializers import GoalSerializer

class GoalViewSet(viewsets.ModelViewSet):
    """
    Gestión de metas simple. La lógica de cálculo reside en signals.py.
    """
    serializer_class = GoalSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        # SIMPLICIDAD ABSOLUTA: Solo devolvemos lo que hay en la DB.
        # El signal ya se encargó de que current_amount sea 250.
        return Goals.objects.filter(
            user=self.request.user,
            is_active=True
        ).order_by('-year', '-month')

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

def goals_page_view(request):
    """Vista para renderizar la página HTML de metas."""
    return render(request, 'goals.html')