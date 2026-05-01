from rest_framework import serializers
from django.db import transaction  # Necesario para sincronizar con la DB
from ..models import Transaction
from ..signals import update_all_user_goals  # Asegúrate de que esta función existe en signals.py

class TransactionSerializer(serializers.ModelSerializer):
    """
    Serializer para la gestión de ingresos y gastos.
    Incluye soporte para categorías y metas vinculadas.
    Fuerza la actualización de metas tras cada operación exitosa.
    """
    category_name = serializers.ReadOnlyField(source='category.name')
    goal_name = serializers.ReadOnlyField(source='goal.name')
    user_email = serializers.ReadOnlyField(source='user.email')

    class Meta:
        model = Transaction
        fields = [
            'id', 'user', 'user_email', 'amount', 'description',
            'type', 'date', 'category', 'category_name', 'goal', 'goal_name'
        ]
        read_only_fields = ['user']

    def validate_amount(self, value):
        if value <= 0:
            raise serializers.ValidationError("El importe debe ser una cantidad positiva.")
        return value

    def to_representation(self, instance):
        """
        Modificado para asegurar el formato de moneda y tipos en la respuesta.
        """
        representation = super().to_representation(instance)

        if representation.get('type'):
            representation['type'] = representation['type'].upper()

        if representation.get('amount'):
            representation['amount'] = f"{float(representation['amount']):.2f}"

        return representation

    def create(self, validated_data):
        """
        Al crear una transacción, esperamos al commit para actualizar metas.
        """
        instance = super().create(validated_data)
        user = instance.user
        # on_commit garantiza que update_all_user_goals vea los datos ya grabados
        transaction.on_commit(lambda: update_all_user_goals(user))
        return instance

    def update(self, instance, validated_data):
        """
        Al actualizar, recalculamos para que los cambios se reflejen de inmediato.
        """
        instance = super().update(instance, validated_data)
        user = instance.user
        transaction.on_commit(lambda: update_all_user_goals(user))
        return instance

    def validate_category(self, value):
        # El usuario que hace la petición
        user = self.context['request'].user
        # Comprobamos si la categoría le pertenece
        if value.user != user:
            raise serializers.ValidationError("Esta categoría no te pertenece.")
        return value