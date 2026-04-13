from django.contrib import admin
from .models import Category, Transaction, Goals

@admin.register(Category)
class CategoriesAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'icon_ref')
    search_fields = ('name',)

@admin.register(Transaction)
class TransactionsAdmin(admin.ModelAdmin):
    # Mostramos las columnas clave que vimos en tu DB
    list_display = ('id', 'user', 'category', 'amount', 'date', 'type')
    list_filter = ('type', 'date', 'category')
    search_fields = ['description']

@admin.register(Goals)
class GoalsAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'user', 'target_amount', 'current_amount', 'deadline')
    # Añadimos un filtro por usuario para que sea fácil de gestionar
    list_filter = ('user',)