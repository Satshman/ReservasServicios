# Observabilidad y pruebas del Sprint 3

Guía para levantar, demostrar y verificar los dos entregables del Sprint 3 de los lineamientos §3.7: *observabilidad con Grafana/Prometheus* y *pruebas de integración*. La decisión de diseño está en el [ADR-0008](adr/0008-observabilidad-con-prometheus-y-grafana.md).

## 1. Observabilidad

### Levantar el stack

```bash
cp .env.example .env    # si no existe; JWT_SECRET (>= 32 bytes) y GRAFANA_ADMIN_PASSWORD son obligatorias
docker compose -f docker-compose.yml -f docker-compose.observabilidad.yml up --build
# con Podman: podman-compose -f docker-compose.yml -f docker-compose.observabilidad.yml up --build
```

| Herramienta | URL | Qué mirar |
|-------------|-----|-----------|
| API | http://localhost:8081/swagger-ui.html | Igual que antes |
| Grafana | http://localhost:3001 (usuario `admin`, clave `GRAFANA_ADMIN_PASSWORD` de `.env`) | Dashboard «Reservas de Servicios», abre como inicio |
| Prometheus | http://localhost:9390 | *Status → Targets* (`reservas-servicios` en `UP`), *Alerts* (8 reglas) |
| Alertmanager | http://localhost:9393 | Alertas activas |
| Mailpit | http://localhost:8025 | Correos de las alertas (y de verificación de cuenta) |

Los puertos del host 9390 y 9393 se eligieron porque Windows suele reservar el rango 9015–9314 (Hyper-V), donde caen los habituales 9090/9093. Se cambian con `PROMETHEUS_HOST_PORT`, `ALERTMANAGER_HOST_PORT` y `GRAFANA_HOST_PORT`.

### Cómo funciona

- El perfil `observabilidad` (`src/main/resources/application-observabilidad.yml`) mueve el actuador al **puerto de gestión 8091**, que no se publica en el host. Con el perfil activo, `/actuator/health` y `/actuator/prometheus` responden solo en ese puerto.
- `/actuator/prometheus` se lee sin token **solo por el puerto de gestión** (`SeguridadMetricasConfig`). Por el puerto de la API responde 401.
- `ConvencionMetricasHttp` agrega a `http_server_requests_seconds_*` la etiqueta `codigo_error` con el `CodigoError` de la regla de negocio que rechazó la petición. De ahí salen las métricas de negocio del dashboard.

### Dashboard

| Fila | Paneles |
|------|---------|
| API | Estado de la app, peticiones/s, % de errores 5xx, latencia p95; peticiones y p95 por endpoint; respuestas por código HTTP |
| Negocio | Reservas creadas y canceladas; reservas rechazadas por regla (`TURNO_SIN_CUPO`, `RECURSO_NO_DISPONIBLE`, `RESERVA_SOLAPADA`…); inicios de sesión exitosos, fallidos y bloqueados; errores de negocio por código |
| Recursos | Heap de la JVM, pool de conexiones Hikari (activas, libres, en espera), CPU |

### Alertas (`observabilidad/prometheus/alertas.yml`)

| Alerta | Condición | Severidad |
|--------|-----------|-----------|
| AplicacionCaida | `up == 0` durante 1 min | crítica |
| TasaErrores5xxAlta | > 5 % de respuestas 5xx durante 5 min | crítica |
| LatenciaP95Alta | p95 de un endpoint > 1 s durante 5 min | advertencia |
| PoolConexionesAgotado | conexiones en espera > 0 durante 2 min | advertencia |
| MemoriaHeapAlta | heap > 90 % durante 10 min | advertencia |
| PicoInicioSesionFallido | > 20 `CREDENCIALES_INVALIDAS` en 5 min | advertencia |
| CuentasBloqueadas | > 3 `CUENTA_BLOQUEADA` en 15 min | advertencia |
| DemandaSuperaCapacidad | > 10 `TURNO_SIN_CUPO` en 30 min | informativa |

Cada alerta trae la acción esperada (`accion`). Alertmanager las envía por correo a Mailpit.

### Demostración en vivo

1. Abrir Grafana y Prometheus → *Targets*: `reservas-servicios` en `UP`.
2. Simular un ataque de fuerza bruta. Hay que hacerlo **dos veces con un minuto de diferencia**: Prometheus mide aumentos, y la primera ráfaga solo crea la serie.

   ```bash
   for i in $(seq 1 25); do curl -s -o /dev/null -X POST http://localhost:8081/api/v1/auth/login \
     -H 'Content-Type: application/json' -d '{"email":"x@prueba.co","password":"mala"}'; done
   ```

3. En Grafana, el panel «Inicio de sesión» muestra los rechazos. En Prometheus → *Alerts*, `PicoInicioSesionFallido` pasa a *Firing*. Unos 30 s después llega el correo a Mailpit.
4. Hacer reservas desde Swagger hasta llenar un turno: el panel «Reservas rechazadas por regla de negocio» muestra `TURNO_SIN_CUPO`.
5. Detener la app (`docker compose … stop app`): en 1 minuto se dispara `AplicacionCaida`.

## 2. Pruebas

Todas corren con `./mvnw verify` (en Windows: `mvnw.cmd verify` con `JAVA_HOME` en el JDK 21 y Docker Desktop abierto).

### Pruebas de integración nuevas (PostgreSQL real con Testcontainers)

| Clase | Cubre |
|-------|-------|
| `adapters/in/web/auth/SesionTokenIntegracionTest` | Rotación del refresh token, detección de reutilización (revoca todas las sesiones), logout idempotente (ADR-0006) |
| `adapters/in/web/agenda/ExcepcionDisponibilidadIntegracionTest` | Excepciones de disponibilidad: efecto en la disponibilidad y en las reservas, validación, RBAC, ABAC y eliminación |
| `adapters/in/web/reporte/ReporteIntegracionTest` | HU-12 ocupación por día y por bloque; HU-13 desempeño por servicio; validación y propiedad |
| `infrastructure/observabilidad/ObservabilidadIntegracionTest` | Arranca la app en puertos reales con el perfil `observabilidad`: Prometheus lee sin token por el puerto de gestión, la etiqueta `codigo_error` aparece, el puerto de la API no expone métricas |
| `infrastructure/observabilidad/ConvencionMetricasHttpTest` | Unitaria (AAA) de la etiqueta y del filtro por puerto |

### Pruebas de aceptación del backend (Gherkin + Cucumber)

- Escenarios en `src/test/resources/features/` (en español, `# language: es`):
  - `reservas.feature`: HU-07, HU-08 y HU-15 (reserva, sobrecupo con sugerencias, cancelación dentro y fuera de plazo, cancelación del proveedor).
  - `inicio_sesion.feature`: HU-03 y ADR-0006 (login, bloqueo tras 5 intentos, vencimiento del bloqueo, logout que revoca el refresh token).
- Pasos en `src/test/java/co/reservas/aceptacion/PasosAceptacion.java`. Reutilizan el contexto de `PruebaIntegracion`: contenedor PostgreSQL, reloj ajustable y correo capturado.
- Ejecutor: `AceptacionTest`. Para correr solo estas: `./mvnw test -Dtest=AceptacionTest`.
- El reporte HTML queda en `target/cucumber/aceptacion.html`.
