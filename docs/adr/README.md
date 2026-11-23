# Registro de decisiones de arquitectura (ADR)

Cada decisión arquitectónica relevante se documenta aquí con el formato exigido por los lineamientos §4.4: contexto, alternativas consideradas, decisión, consecuencias, responsable y fecha.

Los lineamientos §3.7 exigen **mínimo 3 ADR priorizados** en el Sprint 1.

## Índice

| # | Título | Estado | Fecha | Ámbito | Responsable |
|---|--------|--------|-------|--------|-------------|
| [0001](0001-arquitectura-hexagonal-con-modulos-por-dominio.md) | Arquitectura hexagonal con módulos por dominio | Aceptada | 2026-09-17 | Estilo arquitectónico, estructura de paquetes, regla de dependencia | Equipo Avanzado — Isaac Mesa |
| [0002](0002-estados-y-catalogos-en-base-de-datos.md) | Estados y catálogos en tablas de base de datos en lugar de enumeraciones en el esquema | Aceptada | 2026-09-17 | Modelo de datos, extensibilidad de catálogos | Equipo Avanzado — Isaac Mesa |
| [0003](0003-control-de-sobreocupacion-con-bloqueo-pesimista.md) | Control de sobreocupación con bloqueo pesimista en la transacción de reserva | Aceptada | 2026-09-17 | Concurrencia, integridad de reservas y recursos | Equipo Avanzado — Isaac Mesa |
| [0004](0004-autenticacion-jwt-sin-estado-y-bloqueo-por-intentos-fallidos.md) | Autenticación con JWT sin estado y bloqueo temporal por intentos fallidos | Aceptada | 2026-09-17 | Seguridad, autenticación, autorización, escalabilidad | Equipo Avanzado — Isaac Mesa |
| [0005](0005-migraciones-versionadas-con-flyway-y-validacion-de-esquema.md) | Migraciones versionadas con Flyway y validación de esquema por Hibernate | Aceptada | 2026-09-17 | Persistencia, gestión del esquema, entornos | Equipo Avanzado — Isaac Mesa |
| [0006](0006-sesiones-revocables-con-tokens-de-refresco-rotativos.md) | Sesiones revocables con tokens de refresco rotativos y detección de reutilización | Aceptada | 2026-11-23 | Seguridad, gestión de sesiones, revocación de credenciales | Equipo Avanzado — Isaac Mesa |
| [0007](0007-agregacion-del-reporte-de-servicios-en-una-funcion-almacenada.md) | La agregación del reporte de desempeño de servicios vive en una función almacenada | Aceptada | 2026-11-23 | Persistencia, ubicación de la lógica de consulta, regla de dependencia | Equipo Avanzado — Isaac Mesa |

## Qué decidió cada sprint

Esta sección existe para responder de un vistazo qué se mantuvo, qué se ajustó y qué es nuevo, que es lo que se pregunta en cada Review.

- **Sprint 1 — ADR-0001 a 0005.** Las cinco decisiones de fundación: estilo arquitectónico, modelo de catálogos, concurrencia de reservas, autenticación y gestión del esquema.
- **Sprint 2 — ninguna decisión nueva.** Las historias de cancelación e historiales (HU-08, HU-10, HU-11, HU-14, HU-15) se implementaron dentro de las decisiones vigentes, sin alterar ninguna: una tabla nueva y dos índices, cero cambios a tablas existentes. No hay ADR porque no hubo decisión arquitectónica que registrar.
- **Sprint 3 — ADR-0006 y 0007.** El 0006 cierra la deuda que el 0004 se había declarado a sí mismo sobre revocación de tokens, y reafirma la decisión de CSRF explicando por qué sigue siendo válida. El 0007 resuelve dónde debe vivir la agregación de un reporte y, de paso, el procedimiento almacenado que los lineamientos §3.2 pedían desde el Sprint 1. Las cinco decisiones del Sprint 1 **se mantienen**; ninguna fue reemplazada.

## Convenciones

- **Numeración:** secuencial de cuatro dígitos, sin reutilizar números. El nombre del archivo es `NNNN-titulo-en-kebab-case.md`.
- **Estados:** `Propuesta` → `Aceptada` → `Reemplazada por ADR-NNNN` o `Obsoleta`. Un ADR aceptado no se edita para cambiar la decisión: se escribe uno nuevo que lo reemplace y se actualiza el estado del anterior.
- **Alcance:** se documenta una decisión cuando afecta a la estructura del sistema, a un atributo de calidad o cuando revertirla sería costoso. Las decisiones locales y reversibles no necesitan ADR.
- **Actualización:** los ADR y los diagramas se actualizan en el mismo sprint en que cambia la solución (lineamientos §4.4).

## Documentos relacionados

- `docs/modelo-datos-y-paquetes.md` — diseño aprobado: esquema, paquetes, endpoints, códigos de error y reglas de negocio por historia de usuario.
- `docs/auditoria-arquitectura.md` — auditoría de principios de diseño y cumplimiento de la rúbrica, con la evidencia en código de cada decisión registrada aquí.
- `docs/lineamientos.md` — lineamientos del curso; §3 es vinculante para el equipo avanzado.
