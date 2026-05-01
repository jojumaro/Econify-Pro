from rest_framework import serializers
from ..models import Goals

class GoalSerializer(serializers.ModelSerializer):
    """
    Serializer para la gestión de metas de ahorro y límites de gasto.
    Incluye campos calculados dinámicamente como el porcentaje de progreso.
    """
    # Campo calculado en el modelo (property) que exponemos como solo lectura
    progress_percentage = serializers.ReadOnlyField()

    class Meta:
        model = Goals
        fields = [
            'id', 'name', 'target_amount', 'current_amount',
            'goal_type', 'month', 'year', 'deadline',
            'status', 'is_active', 'progress_percentage'
        ]

        # Seguridad: Campos que el usuario no puede manipular directamente vía API
        read_only_fields = ['user', 'status']

    def validate_target_amount(self, value):
        """
        Validación de Negocio: No tiene sentido una meta de 0 o negativa.
        """
        if value <= 0:
            raise serializers.ValidationError("El objetivo de ahorro debe ser mayor que cero.")
        return value

    def to_representation(self, instance):
        """
        Clean Code: Aseguramos que los valores numéricos siempre 
        lleguen al frontend con un formato limpio.
        """
        representation = super().to_representation(instance)
        # Redondeamos a 2 decimales para evitar problemas de precisión en la App
        representation['current_amount'] = round(float(instance.current_amount), 2)
        representation['target_amount'] = round(float(instance.target_amount), 2)
        return representation