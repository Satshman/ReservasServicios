# ADR-0008 — Observabilidad con Prometheus y Grafana, con métricas de negocio derivadas de la métrica HTTP

- **Estado:** Propuesta
- **Fecha:** 24 de noviembre de 2026
- **Responsable:** Equipo Avanzado — Sara Mesa
- **Ámbito:** Observabilidad, operación, seguridad del endpoint de métricas
- **Relación:** complementa los logs JSON (ECS) y el `traceId` del cuerpo uniforme de errores; no reemplaza ningún ADR

## Contexto

Los lineamientos §3.7 piden para el Sprint 3 *«observabilidad con Grafana/Prometheus»*, y §4.1 describe la capa de observabilidad como *«logs, métricas, trazas, dashboards y alertas accionables»*. Hasta el Sprint 2 el backend tenía logs estructurados en JSON y `traceId` en cada error, pero solo exponía `/actuator/health`: no había métricas, dashboards ni alertas.

Hay tres restricciones:

1. **Seguridad.** Las métricas revelan rutas, volúmenes y errores. No pueden quedar públicas en Internet, y los lineamientos §3.4 exigen RBAC por endpoint.
2. **Arquitectura hexagonal (ADR-0001).** La capa `application` no debe depender de Micrometer ni de ningún framework de métricas.
3. **Despliegue.** El plan gratuito de Render ejecuta un solo contenedor con un solo puerto público, así que no puede alojar Prometheus ni Grafana.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **Contadores de negocio en los servicios** (`MeterRegistry` inyectado en `ReservaService`, `IniciarSesionService`…) | Rompe la regla de dependencia del ADR-0001: Micrometer entraría a `application`. Obliga a cambiar constructores y pruebas unitarias de servicios ya estables. |
| **Puerto de salida `MetricasPort` con adaptador Micrometer** | Respeta la arquitectura, pero exige tocar todos los servicios para llamar al puerto y aprobar un cambio de paquetes en `docs/modelo-datos-y-paquetes.md`. El costo no se justifica si la misma información ya pasa por la capa web. |
| **Exponer `/actuator/prometheus` en el puerto de la API con usuario y clave** | Mezcla credenciales técnicas con el modelo JWT y deja el endpoint alcanzable desde Internet. |
| **Grafana Cloud con *remote write* desde producción** | Requiere cuenta externa y secretos adicionales en Render. Queda como evolución si el curso pide observabilidad en producción. |
| **Trazas distribuidas (OpenTelemetry + Tempo)** | Es un monolito con una sola base de datos; el `traceId` en logs y errores ya permite correlacionar una petición. Se pospone. |

## Decisión

### Métricas

- Se agrega `micrometer-registry-prometheus` y un perfil `observabilidad` (`application-observabilidad.yml`) que expone `health` y `prometheus` con la etiqueta `application` e histogramas de latencia de `http.server.requests` para calcular percentiles.
- **Las métricas de negocio se derivan de `http.server.requests`.** `ConvencionMetricasHttp` (en `infrastructure.observabilidad`) agrega la etiqueta `codigo_error` con el `CodigoError` de la `ExcepcionNegocio` que rechazó la petición, o `NINGUNO`. Así salen sin tocar `application` ni `domain`: reservas creadas (`POST /reservas` 201), canceladas, rechazos por sobrecupo (`TURNO_SIN_CUPO`) o recurso ocupado, logins fallidos (`CREDENCIALES_INVALIDAS`) y cuentas bloqueadas (`CUENTA_BLOQUEADA`). La cardinalidad está acotada por el enum.

### Seguridad del endpoint

- El actuador se sirve en un **puerto de gestión separado** (`MANAGEMENT_PORT`, 8091) que no se publica en el host; solo Prometheus lo alcanza por la red interna de contenedores.
- `SeguridadMetricasConfig` agrega una cadena de seguridad que permite `/actuator/prometheus` sin token **solo si la petición llega por el puerto de gestión**. Por el puerto de la API la ruta sigue protegida por la cadena principal (401).

### Plataforma local

`docker-compose.observabilidad.yml` se superpone al compose base sin modificarlo y agrega:

- **Prometheus:** lee la app cada 15 s y evalúa `observabilidad/prometheus/alertas.yml`. Son 8 reglas en cuatro grupos (disponibilidad, recursos, seguridad y negocio), cada una con severidad, descripción y la acción esperada.
- **Alertmanager:** envía los avisos por correo a Mailpit e inhibe las advertencias mientras la app está caída.
- **Grafana:** carga sola la fuente de datos y el dashboard «Reservas de Servicios», con filas de API, negocio y recursos (JVM y Hikari). La clave de administrador no tiene valor por defecto: sale de `GRAFANA_ADMIN_PASSWORD` en `.env`.

## Consecuencias

**Positivas**

- Cero cambios en `domain`, `application` y en los adaptadores existentes. La observabilidad es un módulo transversal (lineamientos §3.1), aislado en `infrastructure.observabilidad`.
- Toda regla de negocio nueva que lance `ExcepcionNegocio` aparece en las métricas sin código adicional.
- El endpoint de métricas no es alcanzable desde Internet, y Prometheus, Alertmanager y Grafana solo se publican en `127.0.0.1`.
- Pruebas automatizadas cubren el contrato: `ObservabilidadIntegracionTest` (puertos reales) y `ConvencionMetricasHttpTest`.

**Negativas**

- Las métricas de negocio cuentan **peticiones HTTP**, no hechos de dominio. Una reserva creada por otro canal (un job, por ejemplo) no se contaría. Hoy la API es el único canal.
- Con el perfil activo, `/actuator/health` se mueve al puerto de gestión. Por eso el perfil **no** se activa en Render, cuyo *health check* usa el puerto de la API.
- La observabilidad queda en el entorno local (Docker Compose). Producción sigue con logs JSON y `/actuator/health`.

## Fuera de alcance

- Trazas distribuidas con OpenTelemetry.
- Prometheus o Grafana gestionados para el entorno de Render.
- Métricas de dominio que no pasan por HTTP (por ejemplo, gauges de reservas confirmadas por servicio leídos de la base).
