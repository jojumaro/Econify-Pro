from rest_framework import serializers
from django.utils import timezone
from ..models import Transaction, Goals
from .transaction_serializers import TransactionSerializer
from .goal_serializers import GoalSerializer

class DashboardSerializer(serializers.Serializer):
    summary = serializers.SerializerMethodField()
    categories = serializers.SerializerMethodField()
    recent_transactions = serializers.SerializerMethodField()
    goals = serializers.SerializerMethodField()

    def get_summary(self, obj):
        return obj.get('summary')

    def get_categories(self, obj):
        return obj.get('categories')

    def get_recent_transactions(self, obj):
        user = self.context['request'].user
        # Extraemos mes y año pasados desde la vista
        month = self.context.get('month')
        year = self.context.get('year')

        # Si por alguna razón son None, usamos los actuales para no romper
        if not month or not year:
            hoy = timezone.now()
            month, year = hoy.month, hoy.year

        transactions = Transaction.objects.filter(
            user=user,
            date__year=year,
            date__month=month
        ).order_by('-date')[:4]

        return TransactionSerializer(transactions, many=True).data

    def get_goals(self, obj):
        user = self.context['request'].user
        # CONSEJO: Las metas suelen ser a largo plazo.
        # Si las filtras por mes y año, en los meses que no creaste una meta saldrá VACÍO.
        # Mejor mostrar todas las metas ACTIVAS del usuario siempre:
        goals = Goals.objects.filter(user=user, is_active=True).order_by('-id')[:3]
        return GoalSerializer(goals, many=True).data