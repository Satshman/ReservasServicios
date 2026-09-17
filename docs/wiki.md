Wiki - Plataforma de Reservas de Servicios (CodeF@ctory 2026-2)
Equipo Avanzado
Descripción del Proyecto
Plataforma de Reservas de Servicios es un backend robusto integrado a base de datos, con panel web de gestión, diseñado para administrar reservas de servicios y recursos en negocios como clínicas, consultorios, salones de belleza o centros deportivos. El sistema busca resolver la sobreocupación, las cancelaciones desordenadas y la falta de trazabilidad en la gestión de agendas.
En el Sprint 1, el objetivo principal es implementar un MVP que abarque la arquitectura base del proyecto (dominios de negocio bien delimitados), el registro de usuarios/proveedores, la configuración inicial de agendas, el despliegue inicial y los diagnósticos de seguridad exigidos para el nivel avanzado.

Objetivo del Sprint 1
Arquitectura y proyecto base: Diagrama de paquetes y componentes con interfaces, estilo arquitectónico preliminar (justificado), proyecto base Spring Boot en GitHub, al menos 3 ADR priorizados.
Módulos de dominio: Delimitar al menos los módulos Usuarios/Proveedores, Agendas, Reservas y Recursos, más un módulo transversal (autenticación y autorización).
Backlog e Historias: Product Backlog priorizado en Azure DevOps con épicas, features e historias de usuario; al menos una HU implementada de punta a punta (frontend-backend-BD-despliegue).
Base de datos: Entidades y relaciones identificadas, consultas clave, modelo lógico y modelo físico inicial en PostgreSQL.
Seguridad: Diagnóstico inicial SAMM, variables de entorno para credenciales, primeras reglas de autorización (RBAC cliente/proveedor/admin).
Despliegue inicial: Backend y base de datos contenerizados (Docker), desplegados en Render o equivalente.
Ceremonias Scrum y Acuerdos de Equipo
Sprint Planning: 01/09 - 12:00 m
Weekly Scrum / Sync adicional: [Día y hora — por definir]
Daily / Standup: [Sincrónico o asincrónico — por definir]
Sprint Review: 22/09
Retrospectiva: Al finalizar el Sprint 1.
Alcance funcional (Features priorizadas)
#	Feature	Prioridad
1	Gestionar usuarios y proveedores de servicios	Alta
2	Configurar agendas	Alta
3	Gestionar reservas	Alta
4	Controlar disponibilidad de recursos	Alta
5	Consultar historial de reservas	Media
6	Generar reportes de ocupación y uso	Media/Baja
Detalle de historias de usuario, criterios de aceptación y desglose de tareas: ver Azure Boards (Backlog).

Recursos Clave
Recurso	Enlace / Descripción
Repositorio GitHub	https://github.com/Satshman/ReservasServicios 
Azure Boards (Backlog de Historias)	https://dev.azure.com/EAP11FABRICAESCUELA/Plataforma de Reservas de Servicios/_backlogs/backlog/Plataforma de Reservas de Servicios Team/Stories
Azure Boards (Tablero del Sprint)	https://dev.azure.com/EAP11FABRICAESCUELA/Plataforma de Reservas de Servicios/_boards/board/t/Plataforma de Reservas de Servicios Team/Stories
Modelo Entidad-Relación (Base de Datos)	
Documentación de API (OpenAPI/Swagger)	
Análisis de Calidad (SonarCloud/SonarQube)	
Pipeline CI/CD (GitHub Actions)	
Entorno desplegado (Render)	
Stack Tecnológico (perfil avanzado)
Aspecto	Definición
Backend	Spring Boot 3.x+ (JDK 11+)
Base de datos	PostgreSQL (Supabase/Neon como opción administrada)
API	REST u GraphQL, con contrato versionado, validación y documentación (OpenAPI/Swagger)
Despliegue	Render (mínimo), contenedores Docker; Kubernetes como evolución opcional
Gestión del proyecto	Azure DevOps
Repositorio	GitHub
Criterios de Calidad y Definiciones (DoR & DoD)
Definition of Ready (DoR)
Historia redactada en formato: Como [rol] quiero [acción] para [beneficio].
Criterios de aceptación redactados y refinados en Gherkin (Given/When/Then).
Estimación realizada (Story Points) y HU vinculada a su Feature en el backlog.
Dependencias técnicas o de datos identificadas antes de entrar al sprint.
Definition of Done (DoD) — integrada
Historia (HU) aceptada contra criterios funcionales y reglas de negocio.
Código revisado e integrado mediante Pull Request (mínimo 1 aprobación) y pipeline exitoso.
Pruebas pertinentes ejecutadas (unitarias, integración con BD real/contenedor, aceptación) con evidencia disponible.
Quality Gate de SonarCloud/SonarQube cumplido y controles de seguridad (SAST/SCA) sin hallazgos críticos.
Documentación, contrato de API (OpenAPI/Swagger), diagramas y migraciones actualizados cuando aplique.
Accesibilidad (WCAG 2.2 AA) y usabilidad verificadas para cambios de interfaz.
Despliegue comprobado en el entorno definido, con logs y métricas disponibles.
Sin defectos críticos ni vulnerabilidades críticas abiertas.
Diagnóstico Inicial OWASP SAMM
Estrategia y Gobernanza: Controles iniciales de seguridad establecidos; clasificación de datos sensibles (datos de clientes, horarios, historial de reservas).
Desarrollo Seguro: Variables de entorno para credenciales y secretos de base de datos; validación de entradas en la creación de reservas y disponibilidad.
Verificación: Sanitización de parámetros de entrada (fechas, horarios, IDs de recursos) para prevenir inyecciones; revisión de autorización por rol (cliente/proveedor/admin) en cada endpoint.
Operación: Registro de eventos de seguridad (intentos fallidos de login, cambios en reservas) sin exponer datos sensibles en logs.
Requisitos No Funcionales (línea base)
Atributo	Meta inicial
Rendimiento	≥200 solicitudes/minuto, respuesta ≤30s (línea base académica; ajustar por endpoint)
Disponibilidad	Definir objetivo (ej. 99%) con monitoreo básico
Seguridad	RBAC por endpoint; MFA en accesos administrativos
Trazabilidad	Correlación entre HU, commits, pruebas y despliegues
