from django.contrib.auth import get_user_model
from rest_framework import serializers

User = get_user_model()

class UserSerializer(serializers.ModelSerializer):
    """
    Serializer para la gestión de usuarios, incluyendo registro y
    actualización de perfil con hashing de contraseñas.
    """
    password = serializers.CharField(
        write_only=True,
        required=False,  # Requerido en creación, opcional en actualización
        style={'input_type': 'password'}
    )

    class Meta:
        model = User
        fields = [
            'id', 'email', 'password', 'firstname', 'lastname',
            'security_question_1', 'security_answer_1',
            'security_question_2', 'security_answer_2'
        ]

    def create(self, validated_data):
        """Crea un usuario asegurando el hashing de la contraseña."""
        password = validated_data.pop('password')
        return User.objects.create_user(password=password, **validated_data)

    def update(self, instance, validated_data):
        """Actualiza el usuario gestionando correctamente la encriptación de contraseña."""
        password = validated_data.pop('password', None)

        # Actualizamos todos los campos validados de forma dinámica
        for attr, value in validated_data.items():
            setattr(instance, attr, value)

        if password:
            instance.set_password(password)

        instance.save()
        return instance