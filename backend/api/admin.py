from django.contrib import admin
from .models import Category, Transactions, Goals

@admin.register(Category)
class CategoriesAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'icon_ref')
    search_fields = ('name',)

@admin.register(Transactions)
class TransactionsAdmin(admin.ModelAdmin):
    # Mostramos las columnas clave que vimos en tu DB
    list_display = ('id', 'user', 'amount', 'description', 'date', 'is_expense', 'category')
    list_filter = ('is_expense', 'category', 'date')
    search_fields = ('description', 'user__username')

@admin.register(Goals)
class GoalsAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'user', 'target_amount', 'current_amount', 'deadline')
    # Añadimos un filtro por usuario para que sea fácil de gestionar
    list_filter = ('user',)