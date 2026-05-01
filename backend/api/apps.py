from django.apps import AppConfig

class ApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'api'

    def ready(self):
        # Al usar 'import api.signals', Django busca desde la raíz del proyecto
        import api.signals