********************************
*    Requisitos Funcionales    *
********************************
ID	  Requisito	Descripción
RF-01	Gestión de usuarios	El sistema permitirá el registro, inicio de sesión y gestión de perfiles de usuario de forma segura.
RF-02	Registro de transacciones	El usuario podrá añadir, modificar y eliminar ingresos y gastos, asignándoles una categoría y fecha.
RF-03	Gestión de categorías	El sistema ofrecerá categorías predefinidas (alimentación, transporte, ocio) y permitirá la personalización de las mismas.
RF-04	Definición de metas	El usuario podrá crear objetivos de ahorro específicos, asignando una cuantía objetivo y una fecha límite.
RF-05	Vinculación gasto-meta	El sistema permitirá visualizar cómo cada ahorro o gasto afecta porcentualmente a la consecución de una meta activa.
RF-06	Visualización de datos	Generación de gráficos comparativos (mensuales/anuales) y resúmenes visuales del estado financiero.
RF-07	Sincronización en la nube	Los datos estarán centralizados en una base de datos MySQL, permitiendo la persistencia entre la App Android y la Web.
RF-08 Gestión de Roles	El sistema diferenciará entre Usuario Final (capaz de gestionar sus propios gastos y metas) y Administrador (con capacidad para gestionar usuarios globales y configuraciones clave del sistema).
RF-09	Autocompletado	Al introducir el correo electrónico de un usuario registrado en el sistema, la interfaz recuperará automáticamente sus datos de perfil para agilizar procesos de identificación o configuración.

*********************************
*   Requisitos No Funcionales   *
*********************************
ID	    Requisito	Descripción
RNF-01	Usabilidad (UX/UI)	La interfaz debe ser intuitiva y fácil de usar, minimizando el número de pasos para registrar un gasto.
RNF-02	Disponibilidad	El Backend (API Django) debe estar disponible para responder a las peticiones de los clientes (App/Web) de forma constante.
RNF-03	Seguridad	Las contraseñas deben almacenarse encriptadas y la comunicación entre cliente y servidor se establecerá mediante el uso del protocolo HTTPS y autenticación mediante tokens JWT (JSON Web Tokens)
RNF-04	Escalabilidad	La arquitectura debe permitir añadir nuevas funcionalidades (como exportación a PDF) sin afectar al núcleo del sistema.
RNF-05	Rendimiento	Las consultas a la base de datos deben estar optimizadas para que la carga de gráficos no supere los 2 segundos.
