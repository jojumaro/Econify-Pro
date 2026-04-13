from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from django.db.models import Sum
from .models import Transaction, Goals
from datetime import date, datetime


@receiver(post_save, sender=Transaction)
@receiver(post_delete, sender=Transaction)
def refresh_all_user_goals(sender, instance, **kwargs):
    user = instance.user
    print(f"--- Ejecutando Signal para usuario: {user.email} ---")

    # 1. ACTUALIZACIÓN MANUAL (Huchas)
    if instance.goal:
        goal = instance.goal
        total_hucha = Transaction.objects.filter(goal=goal).aggregate(Sum('amount'))['amount__sum'] or 0
        goal.current_amount = float(total_hucha)
        goal.save()
        print(f"Meta Manual '{goal.name}' actualizada a {goal.current_amount}")

    # 2. ACTUALIZACIÓN AUTOMÁTICA (Balance Mensual)
    metas_auto = Goals.objects.filter(user=user, goal_type='AUTO')
    print(f"Metas AUTO encontradas: {metas_auto.count()}")

    for meta in metas_auto:
        # Aseguramos que las fechas sean objetos 'date' puros
        if isinstance(meta.deadline, datetime):
            f_fin = meta.deadline.date()
        else:
            f_fin = meta.deadline

        # Para el cálculo de abril, forzamos inicio el día 1 de ese mes
        if f_fin:
            f_inicio = f_fin.replace(day=1)
        else:
            f_inicio = date(2020, 1, 1)  # Fecha muy antigua si no hay límite

        print(f"Calculando meta '{meta.name}' entre {f_inicio} y {f_fin}")

        # Filtros de transacciones
        ingresos = Transaction.objects.filter(
            user=user,
            type='INCOME',
            date__gte=f_inicio,
            date__lte=f_fin if f_fin else date(2099, 12, 31)
        ).aggregate(Sum('amount'))['amount__sum'] or 0

        gastos = Transaction.objects.filter(
            user=user,
            type='EXPENSE',
            date__gte=f_inicio,
            date__lte=f_fin if f_fin else date(2099, 12, 31)
        ).aggregate(Sum('amount'))['amount__sum'] or 0

        # El balance neto
        meta.current_amount = float(ingresos) - float(gastos)

        # Forzamos el guardado
        meta.save()
        print(f"Resultado Meta AUTO: Ingresos({ingresos}) - Gastos({gastos}) = {meta.current_amount}")