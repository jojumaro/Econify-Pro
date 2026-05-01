from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.tokens import default_token_generator
from django.utils.http import urlsafe_base64_encode, urlsafe_base64_decode
from django.utils.encoding import force_bytes
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from django.shortcuts import render
from django.contrib.auth.decorators import login_required

# Solo importamos el serializer de usuario, los demás van en sus respectivos archivos
from ..serializers.auth_serializers import UserSerializer

User = get_user_model()

SECURITY_QUESTIONS = {
    "1": "¿Nombre de tu primera mascota?",
    "2": "¿Ciudad donde nacieron tus padres?",
    "3": "¿Nombre de tu escuela primaria?",
    "4": "¿Tu comida favorita de la infancia?",
    "5": "¿Modelo de tu primer coche?"
}

@api_view(['POST'])
@permission_classes([AllowAny])
def register_user(request):
    """Crea un nuevo usuario."""
    serializer = UserSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save()
        return Response({"message": "Usuario creado con éxito"}, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class LoginView(APIView):
    """Autentica al usuario por email y devuelve tokens JWT."""
    permission_classes = [AllowAny]

    def post(self, request):
        email = request.data.get('email')
        password = request.data.get('password')
        user_auth = authenticate(username=email, password=password)

        if user_auth:
            refresh = RefreshToken.for_user(user_auth)
            return Response({
                'access': str(refresh.access_token),
                'refresh': str(refresh),
                'email': user_auth.email,
                'firstname': user_auth.firstname,
                'lastname': user_auth.lastname
            }, status=status.HTTP_200_OK)
        return Response({'error': 'Credenciales incorrectas'}, status=status.HTTP_401_UNAUTHORIZED)


class UserProfileView(APIView):
    """Ver y editar el perfil del usuario autenticado."""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        serializer = UserSerializer(request.user)
        return Response(serializer.data)

    def patch(self, request):
        user = request.user
        data = request.data
        user.firstname = data.get('firstname', user.firstname)
        user.lastname = data.get('lastname', user.lastname)

        password = data.get('password')
        if password:
            user.set_password(password)

        user.save()
        serializer = UserSerializer(user)
        return Response(serializer.data)


@api_view(['POST'])
@permission_classes([AllowAny])
def verify_identity(request):
    """Verifica respuestas de seguridad para recuperación de cuenta."""
    email = request.data.get('email')
    ans1 = request.data.get('ans1', '').strip().lower()
    ans2 = request.data.get('ans2', '').strip().lower()

    try:
        user = User.objects.get(email=email)
        if (user.security_answer_1.lower() == ans1 and
                user.security_answer_2.lower() == ans2):
            token = default_token_generator.make_token(user)
            uid = urlsafe_base64_encode(force_bytes(user.pk))
            return Response({"uid": uid, "token": token}, status=status.HTTP_200_OK)

        return Response({"error": "Respuestas incorrectas"}, status=status.HTTP_400_BAD_REQUEST)
    except User.DoesNotExist:
        return Response({"error": "Usuario no encontrado"}, status=status.HTTP_404_NOT_FOUND)


@api_view(['GET'])
@permission_classes([AllowAny])
def get_security_questions(request):
    """
    Devuelve el listado maestro de preguntas con IDs para el REGISTRO.
    Ahora lo genera dinámicamente desde el diccionario.
    """
    # Transformamos el diccionario en una lista de objetos que entiende el Serializer/Frontend
    questions_list = [
        {"id": int(id_key), "question_text": text}
        for id_key, text in SECURITY_QUESTIONS.items()
    ]
    return Response(questions_list)


@api_view(['GET'])
@permission_classes([AllowAny])
def get_user_questions(request):
    """Obtiene los TEXTOS de las preguntas para un usuario específico."""
    email = request.query_params.get('email')
    try:
        user = User.objects.get(email=email)

        # Traducimos el ID (1, 2, etc.) al texto de la pregunta
        # Usamos str() por si en la DB se guardó como número o texto
        q1_text = SECURITY_QUESTIONS.get(str(user.security_question_1), "Pregunta no encontrada")
        q2_text = SECURITY_QUESTIONS.get(str(user.security_question_2), "Pregunta no encontrada")

        return Response({
            "question1": q1_text,
            "question2": q2_text
        })
    except User.DoesNotExist:
        return Response({"error": "Correo no registrado"}, status=404)


@api_view(['POST'])
@permission_classes([AllowAny])
def reset_password_confirm(request):
    """Confirma el cambio de contraseña tras verificar token."""
    uidb64 = request.data.get('uid')
    token = request.data.get('token')
    new_password = request.data.get('password')

    try:
        uid = urlsafe_base64_decode(uidb64).decode()
        user = User.objects.get(pk=uid)
        if default_token_generator.check_token(user, token):
            user.set_password(new_password)
            user.save()
            return Response({"message": "Contraseña actualizada"}, status=200)
        return Response({"error": "Token inválido o expirado"}, status=400)
    except (TypeError, ValueError, User.DoesNotExist):
        return Response({"error": "Datos inválidos"}, status=400)

# --- VISTAS DE NAVEGACIÓN (HTML) ---

def login_page_view(request):
    """Renderiza la página de inicio de sesión."""
    return render(request, 'auth/login.html')

def register_page_view(request):
    """Renderiza la página de registro."""
    return render(request, 'auth/register.html')

def forgot_password_page_view(request):
    """Renderiza la página de 'olvidé mi contraseña'."""
    return render(request, 'auth/forgot-password.html')

def reset_password_page_view(request):
    """Renderiza la página para establecer la nueva contraseña."""
    return render(request, 'auth/reset-password.html')

#@login_required
def profile_page_view(request):
    """Renderiza la página de perfil del usuario (requiere estar logueado)."""
    return render(request, 'auth/profile.html')