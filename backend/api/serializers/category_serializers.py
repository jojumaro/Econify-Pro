from rest_framework import serializers
from ..models import Category

class CategorySerializer(serializers.ModelSerializer):
    """
    Serializer para gestionar las categorías de gastos e ingresos.
    El usuario se marca como solo lectura porque se asigna automáticamente 
    desde la vista (request.user).
    """

    class Meta:
        model = Category
        fields = ['id', 'name', 'description', 'icon_ref', 'user']

        # Seguridad: El cliente no puede decidir a qué usuario pertenece la categoría
        read_only_fields = ['user']

    def validate_name(self, value):
        """
        Ejemplo de validación Clean Code: Evita nombres vacíos o solo espacios.
        """
        if not value.strip():
            raise serializers.ValidationError("El nombre de la categoría no puede estar vacío.")
        return value