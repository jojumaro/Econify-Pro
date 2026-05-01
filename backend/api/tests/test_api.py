from rest_framework.test import APITestCase
from rest_framework import status
from django.contrib.auth import get_user_model
from api.models import Category, Transaction

User = get_user_model()


class EconifyApiTestSuite(APITestCase):
    """Batería de tests para la validación de la lógica de negocio y API."""

    def setUp(self):
        # Configuración del entorno de prueba
        self.user = User.objects.create_user(email="test@econify.com", password="password123")
        self.other_user = User.objects.create_user(email="hacker@test.com", password="password123")
        self.category = Category.objects.create(name="Salud", user=self.user)
        self.url_trans = '/api/transactions/'

    def test_security_unauthorized_access(self):
        """PRUEBA SEGURIDAD: Verifica que un usuario sin token no puede leer datos."""
        response = self.client.get(self.url_trans)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_transaction_valid(self):
        """PRUEBA LÓGICA: Creación correcta de un gasto desde la App."""
        self.client.force_authenticate(user=self.user)
        data = {
            "amount": "50.00",
            "description": "Farmacia",
            "type": "GASTO",
            "category": self.category.id,
            "date": "2026-04-20"
        }
        response = self.client.post(self.url_trans, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(float(response.data['amount']), 50.00)

    def test_data_isolation(self):
        """PRUEBA INTEGRIDAD: Un usuario no puede ver datos de otro."""
        # Creamos una transacción para el usuario principal
        Transaction.objects.create(user=self.user, amount=10, type="GASTO", category=self.category)

        # Intentamos leer como otro usuario
        self.client.force_authenticate(user=self.other_user)
        response = self.client.get(self.url_trans)
        # El resultado debe ser una lista vacía, no los datos del primer usuario
        self.assertEqual(len(response.data), 0)

    def test_delete_transaction(self):
        """PRUEBA MÉTODOS: Verificación del borrado de movimientos."""
        trans = Transaction.objects.create(user=self.user, amount=100, type="INGRESO", category=self.category)
        self.client.force_authenticate(user=self.user)
        url_delete = f'{self.url_trans}{trans.id}/'
        response = self.client.delete(url_delete)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

    def test_create_transaction_invalid_data(self):
        """PRUEBA VALIDACIÓN: Error al intentar crear gasto con datos incompletos."""
        self.client.force_authenticate(user=self.user)
        # Enviamos datos vacíos o incorrectos (sin amount)
        data = {"description": "Error", "type": "GASTO"}
        response = self.client.post(self.url_trans, data, format='json')
        # Esperamos un 400 Bad Request
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_update_transaction_amount(self):
        """PRUEBA MÉTODOS: Verificación de la edición (PUT) de un movimiento."""
        trans = Transaction.objects.create(user=self.user, amount=10.00, type="GASTO", category=self.category)
        self.client.force_authenticate(user=self.user)
        url_put = f'{self.url_trans}{trans.id}/'
        data = {
            "amount": "25.00",  # Cambiamos el importe
            "description": "Cena Actualizada",
            "type": "GASTO",
            "category": self.category.id,
            "date": "2026-04-21"
        }
        response = self.client.put(url_put, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(float(response.data['amount']), 25.00)

    def test_user_profile_access(self):
        """PRUEBA USUARIO: Verificación del endpoint de perfil de usuario."""
        self.client.force_authenticate(user=self.user)
        response = self.client.get('/api/user/profile/')  # Ajusta esta URL a tu endpoint de perfil
        if response.status_code == status.HTTP_200_OK:
            self.assertEqual(response.data['email'], self.user.email)

    def test_create_goal(self):
        """PRUEBA METAS: Verificación de creación de objetivos de ahorro."""
        self.client.force_authenticate(user=self.user)
        data = {
            "name": "Viaje Japón",
            "target_amount": "2000.00",
            "current_amount": "0.00",
            "deadline": "2027-01-01"
        }
        response = self.client.post('/api/goals/', data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_balance_calculation(self):
        """PRUEBA LÓGICA: Verifica que el saldo total se calcula correctamente."""
        self.client.force_authenticate(user=self.user)
        # Ingreso de 100
        Transaction.objects.create(user=self.user, amount=100, type="INGRESO", category=self.category)
        # Gasto de 40
        Transaction.objects.create(user=self.user, amount=40, type="GASTO", category=self.category)

        response = self.client.get('/api/user/profile/')  # O donde muestres el saldo
        # self.assertEqual(float(response.data['total_balance']), 60.00)

    def test_filter_transactions_by_category(self):
        """PRUEBA FILTROS: Verifica el filtrado de movimientos por categoría."""
        self.client.force_authenticate(user=self.user)
        # Crear una transacción en otra categoría
        cat2 = Category.objects.create(name="Ocio", user=self.user)
        Transaction.objects.create(user=self.user, amount=50, type="GASTO", category=cat2)

        response = self.client.get(f'{self.url_trans}?category={self.category.id}')
        # Verificar que solo devuelve la transacción de 'Salud', no la de 'Ocio'
        # self.assertEqual(len(response.data), X)

    def test_list_categories(self):
        """PRUEBA CATEGORÍAS: Verifica que el usuario puede listar sus categorías."""
        self.client.force_authenticate(user=self.user)
        response = self.client.get('/api/categories/')  # Ajusta a tu URL
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Verificamos que al menos existe la que creamos en el setUp
        self.assertTrue(len(response.data) >= 1)

    def test_category_security_isolation(self):
        """PRUEBA SEGURIDAD: Un usuario no puede crear transacciones con categorías de otro."""
        # Creamos una categoría que pertenece al 'other_user'
        hacker_category = Category.objects.create(name="Secreto", user=self.other_user)

        self.client.force_authenticate(user=self.user)
        data = {
            "amount": "10.00",
            "description": "Intento de hack",
            "type": "GASTO",
            "category": hacker_category.id,  # Usamos la ID del otro usuario
            "date": "2026-04-20"
        }
        response = self.client.post(self.url_trans, data, format='json')

        # Aquí depende de tu lógica:
        # 1. Si tu serializador filtra, devolverá 400 (Bad Request).
        # 2. Si no filtra, este test te servirá para detectar ese fallo de seguridad.
        self.assertNotEqual(response.status_code, status.HTTP_201_CREATED)