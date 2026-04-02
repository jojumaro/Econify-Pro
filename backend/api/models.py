from django.db import models
from django.contrib.auth.models import AbstractUser, BaseUserManager


class Category(models.Model):
    id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=100)
    description = models.CharField(max_length=500, null=True, blank=True)
    icon_ref = models.CharField(max_length=255, null=True, blank=True)

    # Usar 'User' con comillas es correcto para Django, ignora el aviso del IDE
    user = models.ForeignKey('User', on_delete=models.CASCADE, related_name='categories', null=True, blank=True)

    class Meta:
        db_table = 'categories'

class CustomUserManager(BaseUserManager):
    def create_user(self, email, password=None, **extra_fields):
        if not email:
            raise ValueError('El email es obligatorio')
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
    # 1. Eliminamos username y configuramos email como único
    username = None
    email = models.EmailField(unique=True, max_length=255)
    firstname = models.CharField(max_length=45, null=True, blank=True)
    lastname = models.CharField(max_length=100, null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True, null=True)

    objects = CustomUserManager()

    # 2. Configuración para que Django use el email para el login
    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = [] # Los campos que pide el createsuperuser aparte del email/pass

    # 3. Solución al error de "clashes" (ERRORS: api.User.groups...)
    groups = models.ManyToManyField(
        'auth.Group',
        related_name='api_user_groups', # Nombre único para MySQL
        blank=True,
        help_text='The groups this user belongs to.'
    )
    user_permissions = models.ManyToManyField(
        'auth.Permission',
        related_name='api_user_permissions', # Nombre único para MySQL
        blank=True,
        help_text='Specific permissions for this user.'
    )

    class Meta:
        db_table = 'users'

    def __str__(self):
        return self.email

class Transactions(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, db_column='user_id')
    category = models.ForeignKey(Category, on_delete=models.SET_NULL, null=True, db_column='category_id')
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    description = models.CharField(max_length=255)
    date = models.DateField(auto_now_add=True)
    is_expense = models.BooleanField(default=True)

    class Meta:
        db_table = 'transactions'

class Goals(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, db_column='user_id')
    name = models.CharField(max_length=100, null=True)
    target_amount = models.DecimalField(max_digits=10, decimal_places=2)
    current_amount = models.DecimalField(max_digits=10, decimal_places=2, default=0)
    deadline = models.DateField(null=True, blank=True)

    class Meta:
        db_table = 'goals'

    @property
    def progress_percentage(self):
        if self.target_amount > 0:
            return (self.current_amount / self.target_amount) * 100
        return 0