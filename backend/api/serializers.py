from django.contrib.auth import get_user_model
from rest_framework import serializers
from .models import Transactions, Goals, Category

User = get_user_model()

class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ['id', 'name', 'description', 'icon_ref', 'user']
        read_only_fields = ['user']

class TransactionSerializer(serializers.ModelSerializer):
    # Mostramos el nombre de la categoría para que Android no reciba solo un ID
    category_name = serializers.ReadOnlyField(source='category.name')
    # Cambiado: Ahora apunta a user.email ya que username no existe
    user_email = serializers.ReadOnlyField(source='user.email')

    class Meta:
        model = Transactions
        fields = [
            'id', 'user', 'user_email', 'amount', 'description',
            'date', 'is_expense', 'category', 'category_name'
        ]
        # Hacemos que el usuario sea de solo lectura para que se asigne
        # automáticamente en la vista (perform_create)
        read_only_fields = ['user']

class GoalSerializer(serializers.ModelSerializer):
    # Incluimos el porcentaje calculado en el modelo
    progress_percentage = serializers.ReadOnlyField()

    class Meta:
        model = Goals
        fields = '__all__'
        read_only_fields = ['user']

class UserSerializer(serializers.ModelSerializer):
    # La contraseña es write_only por seguridad (nunca viaja del servidor al móvil)
    password = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ['id', 'email', 'password', 'firstname', 'lastname']

    def create(self, validated_data):
        """
        Crea el usuario usando create_user para asegurar el hashing
        de la contraseña (PBKDF2/SHA256).
        """
        user = User.objects.create_user(
            email=validated_data['email'],
            password=validated_data['password'],
            firstname=validated_data.get('firstname', ''),
            lastname=validated_data.get('lastname', '')
        )
        return user

    def update(self, instance, validated_data):
        """
        Maneja la actualización del usuario, incluyendo el hashing de contraseña.
        """
        # Actualizamos los campos de texto
        instance.firstname = validated_data.get('firstname', instance.firstname)
        instance.lastname = validated_data.get('lastname', instance.lastname)
        instance.email = validated_data.get('email', instance.email)

        # Si el usuario envió una nueva contraseña, la encriptamos
        password = validated_data.get('password', None)
        if password:
            instance.set_password(password)

        instance.save()
        return instance