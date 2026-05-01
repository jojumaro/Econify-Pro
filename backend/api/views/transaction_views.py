from rest_framework import viewsets
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import render

# Importamos Goals (en plural) que es como se llama en tu models.py
from ..models import Transaction, Category, Goals
from ..serializers.transaction_serializers import TransactionSerializer


class TransactionViewSet(viewsets.ModelViewSet):
    """
    Gestión de ingresos y gastos con asignación automática de usuario.
    """
    serializer_class = TransactionSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Transaction.objects.filter(user=self.request.user)

        month = self.request.query_params.get('month')
        year = self.request.query_params.get('year')
        category = self.request.query_params.get('category')

        if month and year:
            queryset = queryset.filter(date__month=month, date__year=year)
        if category:
            queryset = queryset.filter(category_id=category)

        return queryset.order_by('-date')

    def perform_create(self, serializer):
        """
        Asigna el usuario actual a la transacción al crearla.
        El serializer ya maneja la asociación de IDs de category y goal.
        """
        serializer.save(user=self.request.user)

    def perform_update(self, serializer):
        """Asegura que el propietario no cambie al editar."""
        serializer.save(user=self.request.user)


def transactions_page_view(request):
    """Renderiza la página de transacciones."""
    return render(request, 'transactions.html')