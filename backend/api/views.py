from rest_framework import viewsets, permissions, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from django.contrib.auth import authenticate, get_user_model
from rest_framework_simplejwt.tokens import RefreshToken
from .models import Transaction, Goals, Category
from .serializers import TransactionSerializer, GoalSerializer, CategorySerializer, UserSerializer
from django.utils import timezone
from django.db.models import Sum

User = get_user_model()

# --- VISTAS DE LÓGICA DE NEGOCIO (Transacciones, Metas, Categorías) ---

class TransactionViewSet(viewsets.ModelViewSet):
    """
    Gestión de ingresos y gastos.
    Filtra por usuario y por fecha (mes/año) si se proporcionan.
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
            queryset = queryset.filter(category_id=category)  # <-- Filtrar por ID

        return queryset.order_by('-date')

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

    def perform_update(self, serializer):
        serializer.save(user=self.request.user)

class GoalViewSet(viewsets.ModelViewSet):
    """
    Gestión de metas de ahorro personales.
    """
    serializer_class = GoalSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        hoy = timezone.now().date()
        inicio_mes = hoy.replace(day=1)

        # Obtenemos todas las metas activas
        metas = Goals.objects.filter(user=user, is_active=True)

        for meta in metas:
            # Definimos los filtros base para evitar repetir código
            base_filter = Transaction.objects.filter(user=user, date__gte=inicio_mes, date__lte=hoy)

            if meta.goal_type == 'MONTHLY_SAVING':
                # Usamos alias 'total' para que sea más limpio
                ingresos = base_filter.filter(type='INGRESO').aggregate(total=Sum('amount'))['total'] or 0
                gastos = base_filter.filter(type='GASTO').aggregate(total=Sum('amount'))['total'] or 0
                meta.current_amount = float(ingresos) - float(gastos)

            elif meta.goal_type == 'EXPENSE_LIMIT':
                # Filtramos gastos que contengan el nombre de la meta
                gastos_esp = \
                base_filter.filter(type='GASTO', description__icontains=meta.name).aggregate(total=Sum('amount'))[
                    'total'] or 0
                meta.current_amount = float(gastos_esp)

            else:  # SAVING (Acumulado histórico total)
                ingresos = Transaction.objects.filter(user=user, type='INGRESO').aggregate(total=Sum('amount'))[
                               'total'] or 0
                gastos = Transaction.objects.filter(user=user, type='GASTO').aggregate(total=Sum('amount'))[
                             'total'] or 0
                meta.current_amount = float(ingresos) - float(gastos)

            meta.save()

        return Goals.objects.filter(user=user).order_by('-start_date')

        def perform_create(self, serializer):
            serializer.save(user=self.request.user)

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

    def get(self, request):
        user = request.user
        # Usamos el mismo Serializer que usas en Android para que la respuesta sea idéntica
        serializer = UserSerializer(user)
        return Response(serializer.data, status=status.HTTP_200_OK)

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


from rest_framework.decorators import api_view, permission_classes
from django.db.models.functions import TruncDate


class DashboardSummaryView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        hoy = timezone.now().date()
        inicio_mes = hoy.replace(day=1)

        # 1. Calculamos totales para el summary
        ingresos_mes = \
        Transaction.objects.filter(user=user, type='INGRESO', date__gte=inicio_mes).aggregate(total=Sum('amount'))[
            'total'] or 0
        gastos_mes = \
        Transaction.objects.filter(user=user, type='GASTO', date__gte=inicio_mes).aggregate(total=Sum('amount'))[
            'total'] or 0

        # 2. Preparamos el objeto de datos para el Serializer
        data_obj = {
            "summary": {
                "ingresos": float(ingresos_mes),
                "gastos": float(gastos_mes),
                "balance": float(ingresos_mes) - float(gastos_mes),
                "ahorro_porcentaje": int(((ingresos_mes - gastos_mes) / ingresos_mes) * 100) if ingresos_mes > 0 else 0
            },
            # Las categorías y transacciones las manejará el Serializer automáticamente
            "categories": [],
            "recent_transactions": [],
            "goals": []
        }

        # 3. ¡ESTA ES LA CLAVE! Usamos el Serializer con el contexto del request
        from .serializers import DashboardSerializer
        serializer = DashboardSerializer(data_obj, context={'request': request})

        return Response(serializer.data)


from django.contrib.auth.tokens import default_token_generator
from django.utils.http import urlsafe_base64_encode
from django.utils.encoding import force_bytes


@api_view(['POST'])
@permission_classes([AllowAny])
def verify_identity(request):
    email = request.data.get('email')
    ans1 = request.data.get('ans1', '').strip().lower()
    ans2 = request.data.get('ans2', '').strip().lower()

    try:
        user = User.objects.get(email=email)

        # Comprobamos ambas respuestas
        if (user.security_answer_1.lower() == ans1 and
                user.security_answer_2.lower() == ans2):

            token = default_token_generator.make_token(user)
            uid = urlsafe_base64_encode(force_bytes(user.pk))

            return Response({
                "uid": uid,
                "token": token,
                "message": "Identidad verificada"
            }, status=status.HTTP_200_OK)
        else:
            return Response({"error": "Las respuestas de seguridad son incorrectas"},
                            status=status.HTTP_400_BAD_REQUEST)

    except User.DoesNotExist:
        return Response({"error": "Usuario no encontrado"}, status=status.HTTP_404_NOT_FOUND)

@api_view(['GET'])
@permission_classes([AllowAny])
def get_security_questions(request):
    # Esta lista es la "Única Fuente de Verdad" para Web y App móvil
    questions = [
        "¿Nombre de tu primera mascota?",
        "¿Ciudad donde nacieron tus padres?",
        "¿Nombre de tu escuela primaria?",
        "¿Tu comida favorita de la infancia?",
        "¿Modelo de tu primer coche?"
    ]
    return Response(questions)

@api_view(['GET'])
@permission_classes([AllowAny])
def get_user_questions(request):
    email = request.query_params.get('email')
    if not email:
        return Response({"error": "Email requerido"}, status=400)

    try:
        user = User.objects.get(email=email)
        return Response({
            "question1": user.security_question_1,
            "question2": user.security_question_2
        }, status=200)
    except User.DoesNotExist:
        return Response({"error": "Este correo no está registrado"}, status=404)


from django.contrib.auth.tokens import default_token_generator
from django.utils.http import urlsafe_base64_decode


@api_view(['POST'])
@permission_classes([AllowAny])
def reset_password_confirm(request):
    uidb64 = request.data.get('uid')
    token = request.data.get('token')
    new_password = request.data.get('password')

    try:
        # Decodificamos el ID del usuario
        uid = urlsafe_base64_decode(uidb64).decode()
        user = User.objects.get(pk=uid)

        # Verificamos si el token es válido para este usuario
        if default_token_generator.check_token(user, token):
            user.set_password(new_password)
            user.save()
            return Response({"message": "Contraseña actualizada"}, status=200)
        else:
            return Response({"error": "El enlace ha expirado o es inválido"}, status=400)

    except (TypeError, ValueError, OverflowError, User.DoesNotExist):
        return Response({"error": "Usuario inválido"}, status=400)

from django.shortcuts import render

# Esta vista sirve la página de registro real (HTML)
def register_page_view(request):
    return render(request, 'auth/register.html')

# Esta vista sirve la página de login real (HTML)
def login_page_view(request):
    return render(request, 'auth/login.html')

# Esta vista sirve la página de dashboard real (HTML)
def dashboard_page_view(request):
    return render(request, 'dashboard.html')

# Esta vista sirve la página de profile real (HTML)
def profile_page_view(request):
    return render(request, 'auth/profile.html')

# Esta vista sirve la página de forgot-password real (HTML):
def forgot_password_page_view(request):
    return render(request, 'auth/forgot-password.html')

# Esta vista sirve la página de reset-password real (HTML):
def reset_password_page_view(request):
    return render(request, 'auth/reset-password.html')

# Esta vista sirve la página de categories real (HTML):
def categories_page_view(request):
    return render(request, 'categories.html')

# Esta vista sirve la página de transactions real (HTML):
def transactions_page_view(request):
    return render(request, 'transactions.html')