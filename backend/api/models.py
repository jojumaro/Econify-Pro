from django.db import models
from django.contrib.auth.models import AbstractUser, BaseUserManager
from django.utils import timezone

class CustomUserManager(BaseUserManager):
    def create_user(self, email, password=None, **extra_fields):
        if not email: raise ValueError('El email es obligatorio')
        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(email, password, **extra_fields)

class User(AbstractUser):
    username = None
    email = models.EmailField(unique=True, max_length=255)
    firstname = models.CharField(max_length=45, null=True, blank=True)
    lastname = models.CharField(max_length=100, null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    security_question_1 = models.CharField(max_length=255, null=True, blank=True)
    security_answer_1 = models.CharField(max_length=255, null=True, blank=True)
    security_question_2 = models.CharField(max_length=255, null=True, blank=True)
    security_answer_2 = models.CharField(max_length=255, null=True, blank=True)
    objects = CustomUserManager()
    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = []
    class Meta:
        db_table = 'users'
        app_label = 'api'

class Category(models.Model):
    name = models.CharField(max_length=100)
    description = models.CharField(max_length=500, null=True, blank=True)
    icon_ref = models.CharField(max_length=255, null=True, blank=True)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='categories')
    class Meta:
        db_table = 'categories'

class Goals(models.Model):
    GOAL_TYPES = [
        ('SAVING', 'Ahorro Acumulado'),
        ('MONTHLY_SAVING', 'Ahorro Mensual Recurrente'),
        ('ANNUAL_SAVING', 'Planificación Anual'),
        ('INVESTMENT', 'Hito de Inversión'),
        ('DEBT_REDUCTION', 'Pago de Deuda'),
    ]
    STATUS_CHOICES = [('active', 'Activa'), ('completed', 'Completada'), ('expired', 'Expirada')]

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='goals')
    name = models.CharField(max_length=100)
    target_amount = models.DecimalField(max_digits=10, decimal_places=2)
    current_amount = models.DecimalField(max_digits=10, decimal_places=2, default=0)
    month = models.IntegerField(null=True, blank=True)
    year = models.IntegerField(null=True, blank=True)
    deadline = models.DateField(null=True, blank=True)
    start_date = models.DateField(auto_now_add=True)
    goal_type = models.CharField(max_length=20, choices=GOAL_TYPES, default='SAVING')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='active')
    is_active = models.BooleanField(default=True)

    @property
    def progress_percentage(self):
        if self.target_amount > 0:
            percentage = (float(self.current_amount) / float(self.target_amount)) * 100
            return min(round(percentage, 2), 100.0)
        return 0

    def save(self, *args, **kwargs):
        if float(self.current_amount) >= float(self.target_amount):
            self.status = 'completed'
        else:
            self.status = 'active'
        super().save(*args, **kwargs)

    class Meta:
        db_table = 'goals'

class Transaction(models.Model):
    TRANSACTION_TYPES = [('GASTO', 'Gasto'), ('INGRESO', 'Ingreso')]
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='transactions')
    category = models.ForeignKey(Category, on_delete=models.SET_NULL, null=True, related_name='transactions')
    goal = models.ForeignKey(Goals, on_delete=models.CASCADE, null=True, blank=True, related_name='contributions')
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    description = models.CharField(max_length=255, blank=True)
    date = models.DateField(default=timezone.now)
    type = models.CharField(max_length=7, choices=TRANSACTION_TYPES)
    class Meta:
        db_table = 'transactions'