from django.contrib import admin
from .models import Category, Transaction, Goals


@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    """Panel de administración para Categorías."""
    list_display = ('name', 'user', 'icon_ref')
    list_filter = ('user',)
    search_fields = ('name', 'description')
    ordering = ('user', 'name')


@admin.register(Transaction)
class TransactionAdmin(admin.ModelAdmin):
    """Panel de administración para Transacciones con jerarquía de fechas."""
    list_display = ('date', 'user', 'type', 'amount', 'category', 'description')
    list_filter = ('type', 'date', 'user', 'category')
    # Importante: buscamos por el email del modelo User relacionado
    search_fields = ('description', 'user__email')

    # Crea un navegador temporal (Año > Mes > Día) en la parte superior
    date_hierarchy = 'date'
    ordering = ('-date',)


@admin.register(Goals)
class GoalsAdmin(admin.ModelAdmin):
    """Panel de administración para Metas con cálculo de progreso visual."""
    list_display = (
        'name', 'user', 'goal_type', 'target_amount',
        'current_amount', 'progress_display', 'status', 'is_active'
    )
    list_filter = ('status', 'goal_type', 'user', 'is_active')
    search_fields = ('name', 'user__email')

    # Evitamos que se modifiquen manualmente campos que la lógica del modelo calcula
    readonly_fields = ('current_amount', 'status')

    def progress_display(self, obj):
        """Muestra el porcentaje de progreso directamente en la lista."""
        return f"{obj.progress_percentage}%"

    progress_display.short_description = '% Progreso'