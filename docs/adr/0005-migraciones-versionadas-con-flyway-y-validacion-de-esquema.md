# ADR-0005 — Migraciones versionadas con Flyway y validación de esquema por Hibernate

- **Estado:** Aceptada
- **Fecha:** 17 de septiembre de 2026
- **Responsable:** Equipo Avanzado — Isaac Mesa

## Contexto

Los lineamientos §5.3 exigen definir respaldo, restauración y **migraciones versionadas**, y §3.2 exige modelos lógico y físico normalizados con índices justificados por consultas reales. El entregable de Bases de Datos del Sprint 1 pide modelo lógico y modelo físico inicial en PostgreSQL, y el de Sprint 3 anticipa un MER refinado y un script actualizado.

El esquema es el contrato compartido entre el código, las pruebas de integración con Testcontainers y el entorno desplegado en Render sobre PostgreSQL administrado en Neon. Ese esquema contiene decisiones que un mapeo automático no sabría tomar: `CHECK` de duración entre 0 y 8 horas, `CHECK` que liga tipo de documento y documento, índice único sobre `lower(email)`, `UNIQUE (id_tipo_estado, nombre)` en los estados, y siete índices cada uno justificado por una consulta concreta.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **`spring.jpa.hibernate.ddl-auto=update`** | Genera el esquema desde las anotaciones. Es cómodo en desarrollo y peligroso en cualquier otro lugar: no versiona nada, no produce un script revisable, no borra ni renombra, y no puede expresar índices funcionales como `lower(email)` ni las restricciones `CHECK` del diseño aprobado. Además impide entregar el «script de estructuras» que piden los lineamientos. |
| **`ddl-auto=create-drop` en pruebas y script manual en producción** | Introduce dos esquemas distintos: el que generan las anotaciones y el que ejecuta un operador. Las pruebas dejarían de probar el esquema real, que es precisamente lo que debe validarse. |
| **Liquibase** | Equivalente en capacidades y con mejor soporte de *rollback* y de formatos declarativos (XML, YAML). Se descartó por preferencia de SQL plano: el equipo quiere que el artefacto versionado sea el mismo SQL que se puede revisar, ejecutar y entregar como evidencia de Bases de Datos, sin una capa de abstracción intermedia. |
| **Scripts SQL ejecutados a mano, sin herramienta** | No hay registro de qué se aplicó en cada entorno, ni verificación de integridad (*checksum*), ni reproducibilidad en CI ni en Testcontainers. |

## Decisión

- **Flyway** gestiona el esquema con migraciones SQL versionadas en `src/main/resources/db/migration`. La versión inicial es `V1__esquema_inicial.sql` (estructura completa: tablas, claves foráneas, `CHECK`, `UNIQUE` e índices) y `V2__datos_semilla.sql` (roles, tipos de documento, tipos de estado, estados, categorías de servicio y tipos de recurso).
- **Hibernate solo valida**: `spring.jpa.hibernate.ddl-auto=validate`. La aplicación no crea ni modifica esquema en ningún entorno; si el mapeo JPA y las tablas divergen, el arranque falla.
- **Las migraciones son inmutables**: una vez aplicada, una migración no se edita. Todo cambio de esquema es una migración nueva con número siguiente, y debe reflejarse también en `docs/modelo-datos-y-paquetes.md`, que es la fuente de verdad del diseño.
- **Cada índice lleva en el propio script el comentario de la consulta que lo justifica**, para satisfacer la exigencia de los lineamientos de justificar su aporte y de evitar índices redundantes.
- **Las pruebas de integración corren contra el esquema migrado**, sobre PostgreSQL real en Testcontainers, incluida una prueba que verifica que la aplicación arranca con el esquema validado y que las filas semilla existen en el número esperado.
- Las credenciales de base de datos llegan por variable de entorno en todos los entornos, incluido el blueprint de Render.

## Consecuencias

### Positivas

- El esquema es reproducible y auditable: el mismo SQL se aplica en desarrollo, en las pruebas y en Neon, y Flyway verifica el *checksum* de lo ya aplicado.
- `ddl-auto=validate` convierte cualquier divergencia entre el mapeo JPA y las tablas en un fallo de arranque inmediato, en lugar de un error de datos descubierto en producción.
- El script de estructuras exigido por el entregable de Bases de Datos no hay que redactarlo aparte: es el artefacto que ya se ejecuta.
- Las restricciones que el ORM no sabría generar —índice funcional sobre `lower(email)`, `CHECK` de duración, `CHECK` que liga tipo de documento y documento— quedan expresadas donde deben estar, en la base de datos.
- Los índices quedan documentados junto a su motivo, lo que permite defender cada uno ante el jurado y detectar los que dejen de estar justificados.

### Negativas

- Todo cambio de modelo exige escribir SQL a mano, lo que es más lento que dejar que Hibernate lo genere y exige que el equipo conozca PostgreSQL.
- Las migraciones aplicadas no se pueden corregir: un error obliga a una migración correctiva adicional, con el ruido histórico que eso deja.
- Flyway Community no ofrece `undo`: revertir un cambio de esquema es una migración nueva escrita a mano, y un `DROP COLUMN` mal planificado no es reversible sin respaldo.
- Hay una duplicación inevitable entre el SQL y las anotaciones JPA; `validate` la detecta, pero no la elimina.
- Los datos semilla quedan acoplados al código: el componente que traduce enumeraciones a identificadores depende de que esos nombres existan. Cambiar un nombre en una migración futura sin ajustar el código rompe el arranque, lo que es preferible a un fallo silencioso, pero obliga a tratarlos como parte del contrato.
