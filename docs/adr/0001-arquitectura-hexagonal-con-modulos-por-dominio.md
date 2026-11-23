# ADR-0001 — Arquitectura hexagonal con módulos por dominio

- **Estado:** Aceptada
- **Fecha:** 17 de septiembre de 2026
- **Responsable:** Equipo Avanzado — Isaac Mesa

## Contexto

Los lineamientos del equipo avanzado (§3.1) exigen un backend organizado en dominios o módulos de negocio, con al menos un módulo transversal, y una separación explícita mediante arquitectura limpia, hexagonal, por capas, monolito modular o microservicio delimitado. También exigen justificar el estilo seleccionado.

El valor del sistema no está en el CRUD sino en reglas que deben ser correctas bajo concurrencia: generación de turnos a partir de bloques semanales en la zona horaria del proveedor, prevención de sobreocupación, detección de conflictos de recursos y detección de reservas afectadas al modificar la agenda. Esas reglas necesitan pruebas rápidas y deterministas, y no deberían cambiar porque cambie el ORM, el proveedor de correo o el formato de la API.

El equipo es pequeño y el alcance del Sprint 1 abarca seis módulos funcionales (`auth`, `usuario`, `servicio`, `agenda`, `recurso`, `reserva`). El despliegue objetivo es un contenedor único en Render con PostgreSQL administrado.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **Arquitectura por capas clásica (controller → service → repository con entidades JPA)** | Es la opción más rápida de escribir, pero acopla las reglas de negocio a las entidades JPA: cada prueba de una regla exige contexto de persistencia, y un cambio en el mapeo se propaga al cálculo de turnos. Las reglas de concurrencia y disponibilidad son el núcleo evaluado del proyecto y quedarían mezcladas con detalles de infraestructura. |
| **Microservicios por dominio** | Un servicio por módulo permitiría desplegar por separado, pero introduce consistencia eventual justo donde necesitamos una transacción: crear una reserva bloquea cliente, servicio y recursos a la vez. Además multiplica la operación (varios despliegues, red entre servicios, trazas distribuidas) sin una necesidad demostrada, contra el criterio de los lineamientos §4.3 de no adoptar complejidad injustificada. |
| **Monolito modular sin puertos (paquetes por módulo con acceso directo a JPA)** | Da el agrupamiento por dominio pero no la inversión de dependencias: los casos de uso seguirían dependiendo de Spring Data. Se pierde la posibilidad de sustituir el reloj, el correo o el hasher en pruebas sin levantar el contexto. |
| **Arquitectura limpia con cuatro capas concéntricas estrictas** | Conceptualmente equivalente a la hexagonal para este tamaño, pero añade una capa de casos de uso separada de los servicios de aplicación que, con seis módulos, produciría clases de una sola línea sin beneficio. |

## Decisión

Se adopta **arquitectura hexagonal (puertos y adaptadores)** con el paquete base `co.reservas` y cuatro capas, organizadas internamente por módulo de dominio:

- `domain`: Java puro. Entidades y objetos de valor como `record`, más reglas sin estado (`GeneradorTurnos`, `DetectorReservasAfectadas`, `DetectorConflictosRecursos`). Prohibidos los imports de Spring, JPA y Jackson.
- `application`: `port/in` con interfaces de caso de uso y sus comandos/resultados, `port/out` con los puertos de salida, y `service` con las implementaciones anotadas `@Service` y `@Transactional`. Aquí viven también las reglas ABAC de propiedad (`VerificadorPropiedad`).
- `adapters`: `in/web` con los controladores REST y sus DTO; `out/persistence`, `out/security` y `out/mail` con las implementaciones de los puertos de salida.
- `infrastructure`: clase principal, configuración, cadena de seguridad y filtros transversales.

Regla de dependencia: `domain` solo depende del JDK; `application` solo de `domain`; `adapters` de `application` y `domain`, nunca de otro adaptador; `infrastructure` puede depender de todas. Las entidades JPA nunca salen de `adapters.out.persistence` y los DTO web nunca entran a `application`.

El módulo transversal exigido por los lineamientos es autenticación y autorización, repartido entre `application/service/auth` e `infrastructure/security`.

## Consecuencias

### Positivas

- Las reglas de negocio se prueban sin contenedor ni base de datos; el conjunto completo de 187 pruebas corre en el pipeline local.
- El reloj, el envío de correo y el hasher de contraseñas son sustituibles: las pruebas de integración usan un reloj ajustable y un capturador de notificaciones en lugar de un servidor SMTP.
- La regla de dependencia es verificable con una búsqueda de imports, lo que la hace auditable ante el jurado.
- Cambiar de proveedor SMTP o de algoritmo de hash es un archivo nuevo en `adapters.out`, sin tocar casos de uso.

### Negativas

- Hay mapeo explícito entre tres representaciones del mismo concepto (DTO web, modelo de dominio, entidad JPA). Es código repetitivo y es el precio consciente de la decisión.
- El número de archivos es mayor que en una arquitectura por capas: cada caso de uso aporta una interfaz, un comando, un resultado y una implementación.
- La regla de dependencia hoy se sostiene por revisión humana; no hay una prueba automática (por ejemplo ArchUnit) que falle si alguien la rompe. Queda como deuda técnica declarada.
- Algunos casos de uso acumulan muchas dependencias por la propia naturaleza de la orquestación transaccional; `ReservaService` recibe ocho colaboradores y suprime explícitamente la regla de Sonar correspondiente.
