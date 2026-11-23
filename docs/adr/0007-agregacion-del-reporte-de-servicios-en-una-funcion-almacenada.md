# ADR-0007 — La agregación del reporte de desempeño de servicios vive en una función almacenada

- **Estado:** Aceptada
- **Fecha:** 23 de noviembre de 2026
- **Responsable:** Equipo Avanzado — Isaac Mesa
- **Ámbito:** Persistencia, ubicación de la lógica de consulta, regla de dependencia
- **Relación:** aplica la regla de dependencia del [ADR-0001](0001-arquitectura-hexagonal-con-modulos-por-dominio.md) a un caso límite

## Contexto

HU-13 pide un reporte de desempeño por servicio: total de reservas, canceladas, tasa de cancelación, el servicio más solicitado, y el conjunto ordenado de mayor a menor demanda. Es agregación sobre `tbl_reservas` cruzada con `tbl_servicios` y `tbl_estados`, con un conteo filtrado por estado y un orden que depende del resultado del conteo.

Hay una exigencia específica de los lineamientos §3.2 que seguía pendiente desde el Sprint 1 y que `docs/modelo-datos-y-paquetes.md` §5 arrastraba como deuda: *«incluir al menos un procedimiento almacenado cuando sea coherente con la distribución de responsabilidades definida en la arquitectura»*. La condición importa: el lineamiento no pide un procedimiento a toda costa, pide uno **donde tenga sentido**.

Y hay una tensión real con el [ADR-0001](0001-arquitectura-hexagonal-con-modulos-por-dominio.md), que fijó que el dominio es Java plano y que la regla de dependencia apunta hacia adentro. Bajar lógica a la base de datos es, en principio, mover reglas hacia una tecnología de infraestructura. La pregunta de esta decisión es si **esto** es una regla de negocio o no.

La respuesta es que no lo es. El reporte no decide nada: no valida, no rechaza, no cambia estado. Cuenta filas y las ordena. No hay ninguna invariante que custodiar, y por eso el caso cae del lado donde la base de datos es la herramienta correcta y no una filtración de responsabilidades.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **Traer las reservas y agregar en Java con `Collectors.groupingBy`** | Es lo que haría por omisión la arquitectura, y funciona con los volúmenes del proyecto. Se descartó porque obliga a traer todas las filas de reservas del proveedor al proceso para contarlas, lo que crece con el historial y no con el resultado, y porque dejaría sin cumplir el lineamiento §3.2 sin una razón mejor que la costumbre. |
| **Consulta JPQL o Criteria API con funciones de agregación** | Habría sido la opción natural y mantiene todo en el adaptador. El conteo filtrado por estado (`COUNT(...) FILTER (WHERE ...)`) y el orden por el resultado del conteo se expresan con torpeza en JPQL, y la consulta resultante es más difícil de leer que el SQL equivalente. |
| **Consulta nativa en el adaptador con `@Query(nativeQuery = true)`** | Era la alternativa más cercana y la decisión fue ajustada. Da el mismo SQL y mantiene la consulta en el repositorio de código Java, que es más fácil de descubrir. Se eligió la función almacenada porque el lineamiento pide explícitamente un procedimiento y porque la firma de la función documenta el contrato del reporte en el propio esquema. La contrapartida está asumida en las consecuencias. |
| **Vista materializada** | Daría lecturas más rápidas, pero exige una política de refresco y entrega datos potencialmente viejos. Un reporte que el proveedor consulta para decidir no debería mostrar un estado que ya no es. |
| **Procedimiento (`PROCEDURE`) en vez de función** | Un `PROCEDURE` no devuelve un conjunto de filas de forma natural y obliga a parámetros de salida o a un cursor. Para una consulta de lectura la función que retorna `TABLE` es la forma correcta. |

## Decisión

Se crea **`fn_reporte_desempeno_servicios`** en la migración `V4__funcion_reporte_servicios.sql`, declarada `LANGUAGE sql` y `STABLE`, que recibe el identificador de proveedor y el rango de fechas opcional, y retorna una tabla con una fila por servicio: identificador, nombre, estado, total de reservas, canceladas y tasa de cancelación.

Tres detalles del diseño:

- **`LANGUAGE sql STABLE`** y no `plpgsql`: no hay control de flujo, es una sola sentencia. `STABLE` le dice al planificador que no modifica datos y que devuelve lo mismo dentro de la misma sentencia, lo que le permite optimizar.
- **El rango de fechas es opcional** y se resuelve con `(p_desde IS NULL OR ...)`, para que el mismo reporte sirva con y sin filtro sin duplicar la función.
- **El orden va dentro de la función** (`ORDER BY total_reservas DESC, s.id ASC`), porque el criterio de aceptación de HU-13 pide el orden por demanda y el desempate por identificador mantiene el resultado determinista cuando dos servicios empatan, como ocurre hoy en producción con Coloración completa y Barbería, ambos con 10 reservas.

La función se invoca desde `ReporteServicioRepositoryJpaAdapter`, detrás de `ReporteServicioRepositoryPort`. **La regla de dependencia del ADR-0001 se respeta sin excepción:** el caso de uso `GenerarReporteServiciosUseCase` depende de un puerto, no de la base; el dominio no sabe que existe una función almacenada. Lo que cambió es qué hay al otro lado del puerto, que es precisamente lo que un puerto permite cambiar.

Lo que **no** baja a la base de datos: la autorización. Que un proveedor no pueda pedir el reporte de otro se decide en la capa de aplicación con `VerificadorPropiedad`, igual que en el resto del sistema, y responde `403 ACCESO_DENEGADO`. La función recibe un identificador de proveedor ya validado y confía en él.

## Consecuencias

### Positivas

- Se cumple el lineamiento §3.2 con un caso donde el procedimiento **es** la herramienta adecuada, en lugar de inventar uno para marcar la casilla.
- La agregación se ejecuta donde están los datos y el resultado que viaja es una fila por servicio, no el historial completo de reservas.
- El contrato del reporte queda declarado en el esquema: la firma de la función dice qué entra y qué sale, y eso lo puede leer quien revise la base sin abrir el código Java.
- La regla de dependencia no se rompe, y el caso queda como precedente de cómo tratar la frontera: **agregación sin decisión** puede bajar a la base, una regla de negocio no.

### Negativas

- **Hay lógica de consulta en una migración**, que es el lugar donde menos se la busca. Cambiarla exige una migración nueva, no una edición, porque `V4` ya está aplicada en producción.
- **No se puede probar con una prueba unitaria.** Verificar la función exige una base de datos real, así que su cobertura depende de las pruebas de integración con Testcontainers y no del conjunto rápido.
- Queda un **riesgo de doble fuente de verdad**: si alguien agrega después una regla de negocio dentro de la función, la decisión se habría usado para justificar lo que el ADR-0001 prohíbe. El criterio queda escrito arriba para que esa discusión tenga una referencia.
- Ata esta parte a PostgreSQL. `COUNT(...) FILTER (WHERE ...)` y la función que retorna `TABLE` no son portables a otro motor sin reescribirlas. El ADR-0005 ya había asumido PostgreSQL como motor, así que no es una dependencia nueva, pero sí es una más.
