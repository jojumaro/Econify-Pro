import calendar
from django.utils import timezone
from django.db.models import Sum
from datetime import datetime

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import render

# Subimos un nivel para encontrar modelos y serializadores
from ..models import Transaction
from ..serializers.dashboard_serializers import DashboardSerializer


class DashboardSummaryView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        hoy = timezone.now().date()

        # 1. Parámetros con fallback seguro
        try:
            month = int(request.query_params.get('month', hoy.month))
            year = int(request.query_params.get('year', hoy.year))
        except (ValueError, TypeError):
            month, year = hoy.month, hoy.year

        # 2. Rango de fechas
        ultimo_dia = calendar.monthrange(year, month)[1]
        inicio_mes = datetime(year, month, 1).date()
        fin_mes = datetime(year, month, ultimo_dia).date()

        # 3. Queryset filtrado
        qs_periodo = Transaction.objects.filter(
            user=user,
            date__range=[inicio_mes, fin_mes]
        )

        ingresos_mes = qs_periodo.filter(type='INGRESO').aggregate(total=Sum('amount'))['total'] or 0
        gastos_mes = qs_periodo.filter(type='GASTO').aggregate(total=Sum('amount'))['total'] or 0
        balance = float(ingresos_mes) - float(gastos_mes)
        ahorro_p = int(((ingresos_mes - gastos_mes) / ingresos_mes) * 100) if ingresos_mes > 0 else 0

        # Lista de meses para el nombre en español manual (más seguro)
        meses_es = ["", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"]

        data_obj = {
            "summary": {
                "ingresos": float(ingresos_mes),
                "gastos": float(gastos_mes),
                "balance": balance,
                "ahorro_porcentaje": ahorro_p,
                "periodo_nombre": f"{meses_es[month]} {year}"
            },
            "categories": [],
            "recent_transactions": [],
            "goals": []
        }

        # 4. Serialización con el contexto correcto
        serializer = DashboardSerializer(
            data_obj,
            context={
                'request': request,
                'month': month,
                'year': year
            }
        )
        return Response(serializer.data)


# --- VISTA PARA NAVEGADOR (HTML) ---

def dashboard_page_view(request):
    """
    Renderiza la página HTML principal del Dashboard.
    """
    return render(request, 'dashboard.html')


class DashboardPeriodsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        # Obtenemos años únicos de las transacciones del usuario
        years = Transaction.objects.filter(user=request.user) \
            .dates('date', 'year', order='DESC')

        years_list = [year.year for year in years]

        # Si no hay transacciones, enviamos el año actual por defecto
        if not years_list:
            years_list = [timezone.now().year]

        return Response({
            "years": years_list,
            "months": [
                {"id": 1, "name": "Enero"}, {"id": 2, "name": "Febrero"},
                {"id": 3, "name": "Marzo"}, {"id": 4, "name": "Abril"},
                {"id": 5, "name": "Mayo"}, {"id": 6, "name": "Junio"},
                {"id": 7, "name": "Julio"}, {"id": 8, "name": "Agosto"},
                {"id": 9, "name": "Septiembre"}, {"id": 10, "name": "Octubre"},
                {"id": 11, "name": "Noviembre"}, {"id": 12, "name": "Diciembre"}
            ]
        })