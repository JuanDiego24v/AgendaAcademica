# 📚 Agenda Académica - Documentación del Proyecto

**Agenda Académica** es una aplicación web moderna diseñada para estudiantes universitarios y escolares. Su objetivo principal es facilitar el seguimiento y organización de cursos, exámenes y calificaciones. Además, incorpora funcionalidades inteligentes mediante la API de **xAI Grok** para extraer evaluaciones directamente desde los sílabos en PDF y responder dudas en un chatbot integrado.

---

## 🛠️ Arquitectura y Tecnologías Utilizadas

El proyecto está construido bajo una arquitectura monolítica clásica basada en capas con el framework **Spring Boot** y el motor de plantillas **Thymeleaf**.

- **Backend**: Java 21 / Spring Boot 3.5.9
- **Seguridad**: Spring Security 6 (Autenticación y autorización basada en sesiones y base de datos)
- **Base de Datos**: MySQL (persistido mediante Spring Data JPA e Hibernate)
- **Inteligencia Artificial**: API de Grok (Modelo `grok-2-latest` de xAI)
- **Procesamiento de Archivos**: Apache PDFBox 3.0.2 (para la lectura y extracción de texto de documentos PDF)
- **Frontend**: HTML5, Vanilla CSS3 y JavaScript integrado mediante plantillas de Thymeleaf

---

## 📁 Estructura del Proyecto (`src/main/java/proyecto/personal/`)

El código fuente está estructurado de la siguiente forma dentro del paquete `proyecto.personal`:

```text
proyecto/personal/
├── config/
│   └── SecurityConfig.java               # Configuración de seguridad (rutas públicas/privadas, cifrado de contraseñas, login/logout)
├── controller/
│   ├── AuthController.java               # Maneja el flujo de autenticación, inicio de sesión y registro de usuarios
│   ├── CursoController.java              # Controlador para el CRUD de cursos
│   ├── ExamenController.java             # Controlador para el CRUD de exámenes y cálculo de notas
│   ├── HomeController.java               # Controlador para la vista principal y panel general
│   ├── IaController.java                 # Controlador para el chatbot de IA y procesamiento de sílabos
│   └── UsuarioController.java            # Controlador para la edición del perfil y cambio de contraseña
├── DTOs/
│   └── EventoDTO.java                    # DTO para formatear los exámenes como eventos del calendario en JS
├── ia/
│   ├── ChatbotService.java               # Servicio del asistente virtual (filtra preguntas no académicas)
│   ├── GrokClient.java                   # Cliente HTTP que consume la API de xAI (Grok)
│   └── PdfExtractorService.java          # Extrae texto de un PDF y pide a Grok que lo devuelva en formato JSON estructurado
├── model/
│   ├── Curso.java                        # Entidad JPA para los cursos del estudiante
│   ├── Examen.java                       # Entidad JPA para las evaluaciones de un curso (peso, nota, fecha)
│   └── Usuario.java                      # Entidad JPA para los datos del usuario administrador/estudiante
├── repository/
│   ├── CursoRepository.java              # Repositorio JPA para operaciones de BD de Cursos
│   ├── ExamenRepository.java             # Repositorio JPA para operaciones de BD de Exámenes
│   └── UsuarioRepository.java            # Repositorio JPA para operaciones de BD de Usuarios
├── security/
│   └── CustomUserDetailsService.java     # Carga los detalles de usuario de la BD para la autenticación de Spring Security
├── service/
│   ├── CursoService.java                 # Lógica de negocio para cursos
│   ├── ExamenService.java                # Lógica de negocio para exámenes (cálculo de promedios, validaciones de porcentaje)
│   └── UsuarioService.java               # Lógica de negocio para gestión de usuarios
└── AgendaAcademicaApplication.java       # Clase principal de ejecución del proyecto Spring Boot
```

---

## 🌟 Funcionalidades Principales

### 1. Gestión de Cursos y Exámenes (CRUD)
- Los estudiantes pueden agregar, editar y eliminar cursos.
- Dentro de cada curso, se pueden crear exámenes o evaluaciones periódicas (Ej: Parcial, Final, Prácticas).
- Cada examen posee un **peso porcentual** (ej. 20%) y una **nota**. El sistema valida de forma automática que la suma de los pesos de los exámenes de un curso no supere el 100%.

### 2. Extracción de Sílabos con IA (PDF a Agenda)
- Permite subir el archivo PDF del sílabo o la guía docente de un curso.
- El sistema utiliza **Apache PDFBox** para leer el texto plano de las páginas y se lo envía a **Grok (xAI)** con un *System Prompt* diseñado para extraer datos estructurados.
- Grok analiza el texto, localiza las evaluaciones con sus fechas y pesos porcentuales, y retorna un array JSON.
- El backend procesa el JSON y registra de manera automática los exámenes identificados en el curso correspondiente.

### 3. Chatbot Académico Integrado
- Un asistente virtual siempre disponible en la esquina inferior de la pantalla para responder dudas sobre el uso de la web o consultas académicas sencillas de la agenda.
- Posee reglas estrictas para mantener el enfoque de la aplicación: si se le pide resolver tareas, problemas matemáticos o lógica compleja, el chatbot se negará amablemente e indicará que su rol es únicamente asistir en el uso de la Agenda Académica.

### 4. Vistas Web dinámicas (Thymeleaf)
- El diseño responsivo cuenta con un sidebar moderno, widgets interactivos y pestañas dinámicas.
- Las páginas de Thymeleaf estructuradas son:
  - `login.html` & `register.html`: Pantallas de control de acceso.
  - `home.html`: Panel de bienvenida con el resumen general y el chatbot.
  - `cursos.html`: Panel para la administración de asignaturas.
  - `examenes.html`: Gestión de evaluaciones de un curso seleccionado, subida del PDF del sílabo, y listado dinámico.
  - `change-password.html`: Panel de seguridad para actualizar la contraseña del perfil del usuario.

---

## 📋 Requisitos Funcionales (RF)

### 🔑 Gestión de Usuarios y Seguridad
* **RF-01 (Registro de Usuario)**: El sistema debe permitir a nuevos estudiantes registrarse ingresando nombre completo, correo electrónico único y una contraseña segura.
* **RF-02 (Autenticación)**: El sistema debe requerir que los usuarios inicien sesión para acceder a su panel académico. Las contraseñas deben estar cifradas en base de datos.
* **RF-03 (Gestión de Perfil / Contraseña)**: Un usuario autenticado debe poder actualizar su contraseña de acceso verificando su contraseña actual.

### 📚 Gestión de Cursos
* **RF-04 (Crear Cursos)**: El usuario debe poder registrar nuevas asignaturas indicando el nombre del curso.
* **RF-05 (Listar Cursos)**: El sistema debe listar las asignaturas activas registradas por el usuario actual y el progreso general del curso.
* **RF-06 (Eliminar y Editar Cursos)**: El usuario debe poder modificar el nombre de un curso o eliminarlo de su historial (lo cual eliminará en cascada sus evaluaciones asociadas).

### 📝 Gestión de Exámenes y Calificaciones
* **RF-07 (Registrar Evaluación)**: El sistema debe permitir crear exámenes en un curso especificando: nombre del examen, fecha planificada, calificación obtenida (0 a 20) y peso porcentual (0% a 100%).
* **RF-08 (Validación de Peso Total)**: El sistema debe impedir el guardado de un nuevo examen si el peso porcentual de este, sumado al de las evaluaciones existentes del curso, supera el 100%.
* **RF-09 (Cálculo de Notas Proyectadas)**: El sistema debe mostrar el promedio actual del alumno en el curso y calcular cuánto necesita obtener en el resto de evaluaciones para aprobar la asignatura.

### 🤖 Inteligencia Artificial y Procesamiento de Documentos
* **RF-10 (Extracción de Exámenes desde Sílabo PDF)**: El usuario debe poder subir un archivo PDF (sílabo). El sistema extraerá el texto y, mediante la API de Grok, creará automáticamente las evaluaciones (nombres, fechas tentativas y pesos porcentuales) dentro del curso correspondiente.
* **RF-11 (Asistente Virtual / Chatbot)**: El sistema proveerá una interfaz de conversación en tiempo real. El chatbot responderá consultas relacionadas con el funcionamiento de la agenda, rendimiento estudiantil y guías del sitio.
* **RF-12 (Restricción Temática de la IA)**: El chatbot debe rechazar de forma educada consultas fuera del ámbito de la gestión y guía de la aplicación (como resolver exámenes de matemáticas, escribir código, etc.).

---

## ⚙️ Configuración del Entorno (`application.properties`)

Antes de arrancar la aplicación, asegúrate de configurar los siguientes parámetros en `src/main/resources/application.properties`:

1. **Base de Datos MySQL**:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/bd_agendaa
   spring.datasource.username=tu_usuario_mysql
   spring.datasource.password=tu_contraseña_mysql
   spring.jpa.hibernate.ddl-auto=update
   ```

2. **API Key de Grok (xAI)**:
   ```properties
   grok.api.key=TU_API_KEY_DE_GROK
   grok.api.url=https://api.xai.com/v1/chat/completions
   ```

---

## 🚀 Cómo Ejecutar el Proyecto

1. Crea la base de datos en MySQL con el nombre configurado (ej: `bd_agendaa`).
2. Abre una terminal en la raíz del proyecto.
3. Ejecuta el comando para compilar y arrancar la aplicación en modo desarrollo:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Abre tu navegador e ingresa a `http://localhost:8080` para comenzar a usar la Agenda Académica.
