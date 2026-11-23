# ADR-0002 — Estados y catálogos en tablas de base de datos en lugar de enumeraciones en el esquema

- **Estado:** Aceptada
- **Fecha:** 17 de septiembre de 2026
- **Responsable:** Equipo Avanzado — Isaac Mesa

## Contexto

El sistema maneja dos familias de valores cerrados con naturalezas distintas:

1. **Catálogos de sistema**: roles (`CLIENTE`, `PROVEEDOR`, `ADMIN`) y estados de usuario, servicio y reserva. Controlan reglas de negocio y de autorización; no los edita un negocio.
2. **Catálogos de negocio**: categorías de servicio, tipos de recurso y tipos de documento. La plataforma es agnóstica al negocio —una clínica, una peluquería o un gimnasio se registran igual—, así que este conjunto debe poder crecer sin que el equipo toque código.

Los lineamientos §5.1 exigen modelos normalizados con claves, restricciones e integridad referencial, y §5.3 exige migraciones versionadas. Las reservas y el historial deben referenciar su estado con integridad referencial garantizada.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **Tipos `ENUM` nativos de PostgreSQL** | Agregar un valor exige `ALTER TYPE ... ADD VALUE`, que en PostgreSQL no puede ejecutarse dentro de un bloque transaccional en todas las versiones y complica las migraciones de Flyway. Reordenar o retirar valores es aún peor. Además no permite adjuntar metadatos (por ejemplo, el tipo al que pertenece un estado). |
| **`varchar` con `CHECK (estado IN (...))`** | Cada valor nuevo exige alterar la restricción, es decir una migración de esquema por cada dato nuevo, y no ofrece integridad referencial real ni un lugar donde listar las opciones para la API de catálogos. |
| **Solo enumeraciones de Java, con el nombre persistido como texto sin tabla** | Sencillo, pero pierde integridad referencial: una fila podría quedar con un estado inexistente por un `UPDATE` manual. Tampoco permite exponer `GET /catalogos/*` desde la base de datos. |
| **Tablas de catálogo para todo, sin enumeraciones en Java** | Obligaría a que las reglas de negocio comparen cadenas o identificadores numéricos (`if (idEstado == 3)`), lo que es frágil, ilegible y no lo verifica el compilador. |

## Decisión

Se adopta un **esquema híbrido con una separación explícita entre las dos familias**:

- Todos los valores cerrados viven en tablas: `tbl_roles`, `tbl_tipos_documento`, `tbl_tipos_estado`, `tbl_estados`, `tbl_categorias_servicio` y `tbl_tipos_recurso`, con claves foráneas reales desde `tbl_usuarios`, `tbl_servicios`, `tbl_reservas` y `tbl_historial_reservas`. `tbl_estados` se subdivide por `id_tipo_estado` con `UNIQUE (id_tipo_estado, nombre)`, de modo que `ACTIVO` de usuario y `ACTIVO` de servicio son filas distintas sin ambigüedad.
- Los **catálogos de sistema** tienen además una enumeración en Java (`Rol`, `EstadoUsuario`, `EstadoServicio`, `EstadoReserva`) que es la fuente de verdad de las reglas. Cada enumeración declara la constante `TIPO` que la liga a su tipo de estado.
- La traducción entre ambos mundos se concentra en un único componente del adaptador de persistencia, `CatalogoSistema`, que resuelve los identificadores **por nombre** contra las filas semilla y los cachea en memoria, porque no cambian en tiempo de ejecución. Si falta una fila esperada, falla de forma explícita en lugar de propagar un `null`.
- Los **catálogos de negocio** no tienen enumeración: se leen de la tabla y se exponen en `GET /api/v1/catalogos/{categorias,tipos-recurso,tipos-documento}`.
- Las filas semilla se crean en una migración versionada (`V2__datos_semilla.sql`), no por código de arranque.

## Consecuencias

### Positivas

- Un negocio nuevo con una categoría de servicio o un tipo de recurso inexistente se atiende con un `INSERT`, sin desplegar código.
- La integridad referencial la garantiza la base de datos: no puede existir una reserva con un estado que no esté en `tbl_estados`.
- Las reglas de negocio se escriben contra enumeraciones de Java, así que el compilador detecta un estado no contemplado; el `switch` de `CodigosHttp` es exhaustivo por la misma razón.
- El acoplamiento con los identificadores numéricos queda confinado a una sola clase, `CatalogoSistema`; ni el dominio ni la capa de aplicación conocen ids de catálogo.
- Los catálogos son consultables por la API, lo que permite que cualquier cliente construya formularios sin valores quemados.

### Negativas

- Existe una dependencia implícita entre el código y los datos semilla: si una migración futura renombra `PENDIENTE_VERIFICACION`, la aplicación falla al arrancar. Se mitiga con la prueba de integración que verifica el número de filas semilla y con el fallo explícito de `CatalogoSistema`.
- Hay una indirección adicional en cada escritura y lectura (nombre ↔ identificador), con su coste de mantenimiento.
- La caché en memoria asume que los catálogos de sistema no cambian mientras la aplicación está viva; agregar un estado nuevo exige reiniciar. Es aceptable porque agregar un estado implica también cambiar código.
- Los catálogos de negocio quedan fuera del alcance del compilador: un identificador de categoría inexistente solo se detecta en tiempo de ejecución, y por eso se valida explícitamente antes de crear un servicio o un recurso.
