from rest_framework import viewsets, permissions, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from django.contrib.auth import authenticate, get_user_model
from rest_framework_simplejwt.tokens import RefreshToken

from .models import Transactions, Goals, Category
from .serializers import TransactionSerializer, GoalSerializer, CategorySerializer, UserSerializer

User = get_user_model()

# --- VISTAS DE LÓGICA DE NEGOCIO (Transacciones, Metas, Categorías) ---

class TransactionViewSet(viewsets.ModelViewSet):
    """
    Gestión de ingresos y gastos.
    Aplica Seguridad 2.2: Cada usuario solo ve sus propios datos.
    """
    serializer_class = TransactionSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Transactions.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

class GoalViewSet(viewsets.ModelViewSet):
    """
    Gestión de metas de ahorro personales.
    """
    serializer_class = GoalSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Goals.objects.filter(user=self.request.user)

from rest_framework import viewsets, permissions
from .models import Category
from .serializers import CategorySerializer

class CategoryViewSet(viewsets.ModelViewSet):
    queryset = Category.objects.all()
    serializer_class = CategorySerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # ESTO ES CLAVE: Veremos en la consola qué usuario detecta el Token
        print(f"--- VALIDANDO TOKEN ---")
        print(f"Usuario detectado: {self.request.user}")
        print(f"¿Está autenticado?: {self.request.user.is_authenticated}")
        print(f"--- CABECERA AUTHORIZATION: {self.request.headers.get('Authorization')}")
        print(f"--- USUARIO DETECTADO: {self.request.user}")

        # Filtramos para que cada usuario solo vea sus categorías
        if self.request.user.is_authenticated:
            return Category.objects.filter(user=self.request.user)
        return Category.objects.none()

    def perform_create(self, serializer):
        # Asignamos el usuario actual al guardar
        serializer.save(user=self.request.user)

# --- VISTAS DE AUTENTICACIÓN (Registro y Login JWT) ---
@api_view(['POST'])
@permission_classes([AllowAny])
def register_user(request):
    """
    Crea un nuevo usuario usando el Serializer para validar los datos.
    """
    serializer = UserSerializer(data=request.data)

    if serializer.is_valid():
        serializer.save()
        return Response(
            {"message": "Usuario creado con éxito"},
            status=status.HTTP_201_CREATED
        )

    # IMPORTANTE: Esto imprimirá el error real en la terminal de Django
    # (Ejemplo: "password: This field is too short")
    print("ERRORES DE VALIDACIÓN:", serializer.errors)

    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class LoginView(APIView):
    """
    Autentica al usuario por email y devuelve tokens JWT (Access y Refresh).
    """
    permission_classes = [AllowAny]

    def post(self, request):
        email = request.data.get('email')
        password = request.data.get('password')

        # Django usa 'username' internamente, pero buscará en el campo 'email'
        # debido a nuestro USERNAME_FIELD en el modelo.
        user_auth = authenticate(username=email, password=password)

        if user_auth:
            from django.contrib.auth import get_user_model
            User = get_user_model()
            user = User.objects.get(pk=user_auth.pk)

            # Generamos el par de tokens JWT
            refresh = RefreshToken.for_user(user)

            print(f"--- DEBUG DJANGO ---")
            print(f"¿Tiene atributo firstname?: {hasattr(user_auth, 'firstname')}")
            print(f"Contenido de firstname: {user_auth.firstname}")
            print(f"Contenido de email: {user_auth.email}")
            print(f"---------------------")

            return Response({
                'access': str(refresh.access_token),
                'refresh': str(refresh),
                'email': user.email,
                'firstname': user.firstname,
                'lastname': user.lastname
            }, status=status.HTTP_200_OK)
        else:
            return Response(
                {'error': 'Credenciales incorrectas'},
                status=status.HTTP_401_UNAUTHORIZED
            )


# --- VISTA PARA EDITAR PERFIL ---

class UserProfileView(APIView):
    """
    Permite al usuario autenticado ver y editar su propio perfil.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request):
        user = request.user
        data = request.data

        # Actualizamos solo los campos que vengan en la petición
        user.firstname = data.get('firstname', user.firstname)
        user.lastname = data.get('lastname', user.lastname)

        # Si viene una contraseña, la encriptamos antes de guardar
        password = data.get('password')
        if password:
            user.set_password(password)

        user.save()

        # Devolvemos los datos actualizados usando tu serializer
        serializer = UserSerializer(user)
        return Response(serializer.data, status=status.HTTP_200_OK)