from django.db.models import Sum, Q, F
from django.db import transaction
from .models import Transaction, Goals
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from django.utils import timezone


def update_all_user_goals(user):
    user_id = user.id
    # Quitamos el atomic de aquí para que cada update sea instantáneo
    metas = Goals.objects.filter(user=user, is_active=True)

    for g in metas:
        if g.goal_type in ['MONTHLY_SAVING', 'ANNUAL_SAVING']:
            qs = Transaction.objects.filter(user=user)
            if g.goal_type == 'MONTHLY_SAVING' and g.month and g.year:
                qs = qs.filter(date__month=g.month, date__year=g.year)

            res = qs.aggregate(
                i=Sum('amount', filter=Q(type='INGRESO')),
                e=Sum('amount', filter=Q(type='GASTO'))
            )
            nuevo_monto = float(res['i'] or 0) - float(res['e'] or 0)
        else:
            # Caso DEBT_REDUCTION
            # Filtramos estrictamente por el ID de la meta
            suma_vinculada = Transaction.objects.filter(goal_id=g.id).aggregate(s=Sum('amount'))['s'] or 0
            nuevo_monto = abs(float(suma_vinculada))

        nuevo_status = 'completed' if nuevo_monto >= float(g.target_amount) else 'active'

        # USAMOS SAVE DIRECTO PERO SIN SEÑALES RECURSIVAS
        Goals.objects.filter(id=g.id).update(current_amount=nuevo_monto, status=nuevo_status)

        # LOG DE VERIFICACIÓN
        if g.id == 11:
            # ESTO COMPRUEBA SI EN LA DB HA ENTRADO EL DATO
            check = Goals.objects.get(id=11)
            print(f"!!! META 11 COMPROBACIÓN POST-UPDATE: {check.current_amount} !!!")


@receiver(post_save, sender=Transaction)
@receiver(post_delete, sender=Transaction)
def update_goals_on_transaction_change(sender, instance, **kwargs):
    # LLAMADA DIRECTA, SIN ESPERAR AL COMMIT
    update_all_user_goals(instance.user)


@receiver(post_save, sender=Goals)
def gestionar_monto_inicial_deuda(sender, instance, created, **kwargs):
    """Crea la transacción inicial al crear la meta."""
    if created and float(instance.current_amount) > 0:
        tipo = 'GASTO'

        Transaction.objects.create(
            goal=instance,
            user=instance.user,
            amount=instance.current_amount,
            type=tipo,
            description=f"Aporte inicial: {instance.name}",
            date=timezone.now().date()
        )