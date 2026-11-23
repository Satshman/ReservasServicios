# ADR-0003 — Control de sobreocupación con bloqueo pesimista en la transacción de reserva

- **Estado:** Aceptada
- **Fecha:** 17 de septiembre de 2026
- **Responsable:** Equipo Avanzado — Isaac Mesa

## Contexto

La sobreocupación es el problema de negocio que motiva la plataforma: `docs/contexto.md` la nombra como la primera dolencia a resolver. Los lineamientos §3.1 exigen reglas de negocio que no se reduzcan a CRUD, y la rúbrica evalúa que las funcionalidades estén «implementadas, integradas, probadas y operativas».

Al crear una reserva concurren tres invariantes que deben evaluarse sobre un estado consistente:

1. **Cupo del turno**: el número de reservas `CONFIRMADA` con el mismo inicio no puede alcanzar la `capacidad` del servicio (1 para una cita individual, N para una clase grupal).
2. **Recursos**: la reserva ocupa **todos** los recursos activos asociados al servicio, y un recurso está ocupado si otra reserva confirmada lo usa en un intervalo que se solapa, salvo que sea del mismo servicio y el mismo inicio (turno grupal compartido).
3. **Cliente**: un cliente no puede tener dos reservas confirmadas que se crucen en el tiempo.

Sin control de concurrencia, dos peticiones simultáneas leen el mismo conteo, ambas concluyen que hay cupo y ambas insertan.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **Restricción `UNIQUE` sobre `(id_servicio, fecha_hora_inicio, id_cliente)`** | Solo impide que el mismo cliente duplique el mismo turno. No expresa un límite de N reservas por turno ni cubre los recursos. |
| **Restricción de exclusión con `tstzrange` y `EXCLUDE USING gist`** | Resuelve elegantemente el solapamiento de un recurso, pero no admite el caso de la clase grupal, donde varias reservas comparten el mismo recurso en el mismo turno de forma intencional, ni expresa un conteo variable por servicio. Habría exigido una condición parcial compleja, difícil de justificar y de probar. |
| **Bloqueo optimista con columna `version` en el servicio** | Evita mantener bloqueos abiertos, pero bajo contención real —el escenario que importa, varios clientes disputando el último cupo— degenera en reintentos y en un `409` para el perdedor que además ya consumió trabajo. La contención esperada es corta y localizada, justo donde el bloqueo pesimista es más eficiente. |
| **Serializar en la aplicación con un cerrojo en memoria** | No sobrevive a más de una instancia; contradice el objetivo de escalar horizontalmente con una aplicación sin estado. |
| **Nivel de aislamiento `SERIALIZABLE` para la transacción** | Correcto, pero traslada el problema a fallos de serialización que el cliente debe reintentar, y penaliza también las operaciones que no compiten. El bloqueo dirigido es más predecible. |

## Decisión

La creación de una reserva se ejecuta en una sola transacción que adquiere **bloqueos pesimistas (`SELECT … FOR UPDATE`) en un orden de adquisición fijo**:

1. La fila del **cliente**, para serializar las reservas concurrentes del mismo cliente.
2. La fila del **servicio**, para serializar la evaluación de cupo del turno.
3. Las filas de los **recursos** activos del servicio, **ordenadas por identificador ascendente**.

Con los bloqueos tomados se evalúan, en orden, las validaciones: servicio activo, inicio futuro, coincidencia con un turno generado por la agenda, cupo disponible, ausencia de solape del cliente y disponibilidad de recursos. Solo entonces se inserta la reserva, sus recursos y la fila de historial.

El orden fijo cliente → servicio → recursos por id es la medida contra interbloqueos: dos transacciones que compiten por los mismos objetos los solicitan en la misma secuencia, de modo que una espera en lugar de cruzarse con la otra.

De forma complementaria, las restricciones únicas que sí aplican (correo de usuario, nombre de servicio por proveedor, nombre de recurso por proveedor) se mantienen en el esquema y su violación se traduce a un error de negocio, como red de seguridad ante la carrera entre la validación previa y la inserción.

La decisión se respalda con una prueba de integración que lanza varios clientes en paralelo contra el último cupo de un turno y verifica que exactamente uno obtiene la reserva.

## Consecuencias

### Positivas

- La invariante de cupo se cumple bajo concurrencia real, demostrado con una prueba ejecutable y no solo argumentado.
- El orden de adquisición documentado elimina la clase de interbloqueos más probable.
- Las tres invariantes se evalúan en el mismo instante lógico, por lo que los mensajes de error son precisos: `TURNO_SIN_CUPO`, `RESERVA_SOLAPADA` o `RECURSO_NO_DISPONIBLE`, cada uno con sugerencias de turnos alternativos.
- El modelo permite la clase grupal (varias reservas comparten recurso en el mismo turno) sin excepciones especiales en el esquema.

### Negativas

- Las reservas del mismo servicio se serializan: el throughput por servicio está acotado por la duración de la transacción. Con la línea base académica de 200 solicitudes por minuto no es un límite, pero sí lo sería en un escenario de venta masiva de un único turno.
- Una transacción lenta bloquea a las demás sobre el mismo servicio; obliga a mantener la transacción de reserva corta y sin llamadas externas (por eso el correo se envía fuera de ella, ADR-0004 y el patrón de evento posterior al commit).
- El bloqueo de la fila del cliente hace que un mismo cliente no pueda crear dos reservas en paralelo, aunque sean de servicios distintos. Es una restricción aceptada para simplificar la validación de solape.
- `POST /reservas` no es idempotente: dos envíos del mismo formulario, si no se solapan en el tiempo, crean dos reservas. Queda registrado como deuda, con clave de idempotencia como solución prevista.
