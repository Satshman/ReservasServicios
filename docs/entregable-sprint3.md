# Entregable del Sprint 3 — backend

Mapa entre lo que piden los lineamientos §3.7 para el Sprint 3 y dónde está la evidencia en el repositorio:

> *Refinamiento de APIs; contenedores y orquestación cuando aplique; observabilidad con Grafana/Prometheus; pruebas de integración; aseguramiento mediante tokens.*

| Punto | Estado | Evidencia principal |
|-------|--------|---------------------|
| Refinamiento de APIs | Cumplido | §1 de este documento · Swagger UI · `docs/modelo-datos-y-paquetes.md` §2.3 y §3 |
| Contenedores y orquestación cuando aplique | Contenedores cumplido; orquestación justificada como no aplicable | §2 · `Dockerfile` · `docker-compose*.yml` · `render.yaml` · `.github/workflows/cd.yml` |
| Observabilidad con Grafana/Prometheus | Cumplido | [ADR-0008](adr/0008-observabilidad-con-prometheus-y-grafana.md) · [`observabilidad-y-pruebas.md`](observabilidad-y-pruebas.md) §1 |
| Pruebas de integración | Cumplido | §4 · [`observabilidad-y-pruebas.md`](observabilidad-y-pruebas.md) §2 |
| Aseguramiento mediante tokens | Cumplido | §5 · [ADR-0004](adr/0004-autenticacion-jwt-sin-estado-y-bloqueo-por-intentos-fallidos.md), [ADR-0006](adr/0006-sesiones-revocables-con-tokens-de-refresco-rotativos.md), [ADR-0009](adr/0009-mfa-por-correo-para-administradores.md) |

## 1. Refinamiento de APIs

Se mantiene el contrato de los sprints anteriores: versionado por ruta (`/api/v1`), OpenAPI en `/v3/api-docs`, validación de las solicitudes con Bean Validation y un único cuerpo de error (`errorCode`, `message`, `details`, `traceId`, `timestamp`). Sobre esa base, el Sprint 3:

| Cambio | Endpoints |
|--------|-----------|
| Reportes (HU-12, HU-13) | `GET /reportes/ocupacion` (desglose `DIA` o `BLOQUE_HORARIO`) · `GET /reportes/servicios` |
| Excepciones de disponibilidad: días o franjas sin atención, que salen de la disponibilidad y bloquean reservas | `POST` y `GET /servicios/{idServicio}/excepciones` · `DELETE /excepciones/{id}` |
| Paginación opcional de los historiales (deuda del Sprint 2) | `?pagina=&tamano=` en `GET /reservas/historial` y `GET /servicios/historial` |
| Sesiones revocables | `POST /auth/refresh` · `POST /auth/logout` |
| MFA de administradores | `POST /auth/mfa/solicitud` · `POST /auth/mfa/verificacion` |
| Corrección del generador de turnos ante cambios de horario de verano (Bug-84) | Afecta a `GET /servicios/{id}/disponibilidad` y a `POST /reservas` |

Cada operación de Swagger documenta sus códigos de error. RBAC por endpoint en `SecurityConfig`, y ABAC (propiedad del recurso) en `VerificadorPropiedad`.

## 2. Contenedores y orquestación

**Contenedores (cumplido)**

- `Dockerfile` en dos etapas: compila con el JDK y ejecuta con una imagen JRE Alpine mínima, como un usuario sin privilegios (`reservas`).
- `docker-compose.yml`: PostgreSQL 17 con *health check*, Mailpit y la aplicación, que arranca solo cuando la base está sana.
- `docker-compose.observabilidad.yml`: se superpone al anterior y agrega Prometheus, Alertmanager y Grafana, todos publicados solo en `127.0.0.1`.
- Producción: la misma imagen corre en Render (`render.yaml`), con PostgreSQL administrado en Neon. `.github/workflows/cd.yml` despliega y verifica `/actuator/health` después de cada despliegue.

**Orquestación (no aplica en este alcance)**

Los lineamientos §1.3 describen Kubernetes como *«evolución opcional»* y el Sprint 3 la pide *«cuando aplique»*. No se adopta porque:

- Es **un solo servicio sin estado** (el estado está en PostgreSQL y las sesiones en tokens), y Render ya da lo que aportaría un orquestador: reinicio ante fallos, *health check* y despliegue continuo.
- Un clúster de Kubernetes exige un plano de control, *ingress*, gestión de secretos y monitoreo propios, con un costo de operación que el volumen del proyecto no justifica.
- La aplicación ya cumple lo que pediría para escalar horizontalmente: no guarda sesión en memoria y la concurrencia de reservas se serializa en la base de datos (ADR-0003). Escalar a varias réplicas solo requiere más instancias detrás del balanceador de Render.

Compose cubre la orquestación local de varios contenedores, con orden de arranque, salud y redes internas.

## 3. Observabilidad

Ver el [ADR-0008](adr/0008-observabilidad-con-prometheus-y-grafana.md) y la [guía de demostración](observabilidad-y-pruebas.md):

- métricas de la API, de negocio (etiqueta `codigo_error`), de la JVM y de la base de datos;
- un dashboard aprovisionado en Grafana;
- 8 alertas con su acción esperada, enviadas por correo;
- el endpoint de métricas, inaccesible desde fuera de la red interna.

## 4. Pruebas de integración

Corren contra PostgreSQL real (Testcontainers) en `./mvnw verify` y en el CI.

| Clase | Qué cubre |
|-------|-----------|
| `ReservaIntegracionTest`, `AgendaIntegracionTest`, `HistorialIntegracionTest`, `ServicioRecursoIntegracionTest`, `RegistroUsuarioIntegracionTest`, `InicioSesionIntegracionTest`, `ContextoAplicacionTest` | Sprints 1 y 2 |
| `SesionTokenIntegracionTest` | Rotación, reutilización y revocación de refresh tokens |
| `MfaIntegracionTest` | MFA de punta a punta: login rechazado para ADMIN, código por correo, un solo uso, vencimiento, bloqueo por fuerza bruta, aprovisionamiento |
| `ExcepcionDisponibilidadIntegracionTest` | Excepciones de disponibilidad y su efecto en disponibilidad y reservas |
| `ReporteIntegracionTest` | HU-12 y HU-13 con reservas confirmadas y canceladas |
| `ObservabilidadIntegracionTest` | La app en puertos reales: lo que lee Prometheus y lo que no se expone |

Pruebas de aceptación del backend en Gherkin (`src/test/resources/features/`), ejecutadas con Cucumber: reservas y cancelaciones, inicio de sesión y sesiones, MFA de administradores.

## 5. Aseguramiento mediante tokens

| Requisito (lineamientos §3.4) | Implementación |
|-------------------------------|----------------|
| Expiración | Token de acceso JWT HS256 de 30 minutos (`JWT_EXPIRATION`), validado con el reloj del sistema y el emisor |
| Rotación | `POST /auth/refresh`: el refresh token es de un solo uso y se cambia por un par nuevo |
| Revocación | `POST /auth/logout`. Reutilizar un refresh token ya rotado revoca todas las sesiones del usuario |
| Almacenamiento seguro | Refresh tokens y códigos MFA guardados solo como hash SHA-256; contraseñas con BCrypt |
| MFA para administradores | Contraseña y código de 6 dígitos por correo; `/auth/login` rechaza las cuentas ADMIN |
| Bloqueo de cuenta | 5 intentos fallidos (contraseña o código MFA) bloquean 15 minutos (`423`) |
| Secretos por variables de entorno | `JWT_SECRET` (mínimo 32 bytes, la app no arranca sin él), `APP_ADMIN_PASSWORD`, `GRAFANA_ADMIN_PASSWORD` |
| Eventos de seguridad | En los logs JSON, con el id de usuario y sin correo, contraseña, token ni código; también visibles en las métricas y alertas |

Limitación conocida: el token de acceso no se revoca antes de vencer. Se acepta en el ADR-0006 porque dura solo 30 minutos y revisar una lista de revocación en cada petición contradice el diseño sin estado del ADR-0004.
