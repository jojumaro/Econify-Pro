# api/tests/test_models.py
from django.test import TestCase
from ..models import Goals, Transaction

class GoalIntegrationTest(TestCase):
    def test_signal_updates_goal(self):
        # INTEGRACIÓN: Probamos que Transaction + Signal + Goal funcionan juntos
        ...