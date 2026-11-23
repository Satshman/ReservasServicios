# Auditoría de arquitectura y calidad — Sprint 1

**Proyecto:** Plataforma de Reservas de Servicios · **Equipo:** Avanzado — Isaac Mesa
**Fecha:** 21 de septiembre de 2026 · **Rama auditada:** `feat/sprint1-backend-reservas` (HEAD `1b75421`)
**Alcance:** `src/main/java/co/reservas/**`, `src/test/java/co/reservas/**`, `src/main/resources/db/migration`, configuración de despliegue y documentación del repositorio.

Cada afirmación se sustenta en una cita `ruta/Archivo.java:línea` verificada sobre el código de esta rama. Cuando un principio se cumple parcialmente se indica de forma explícita, junto con la brecha.

**Resultado de `./mvnw verify` reportado para esta rama:** 187 pruebas, 0 fallos, 97,3 % de cobertura de líneas total y 68,9 % contando solo pruebas unitarias.

---

## 1. SOLID

### 1.1 SRP — Responsabilidad única

| Evidencia | Qué hace |
|-----------|----------|
| `src/main/java/co/reservas/domain/agenda/GeneradorTurnos.java:21` | `generar(...)` solo produce los turnos de un rango de fechas a partir de los bloques semanales; no consulta la base de datos ni conoce reservas. |
| `src/main/java/co/reservas/adapters/in/web/error/CodigosHttp.java:14` | `estado(CodigoError)` tiene una única razón de cambio: la tabla de correspondencia entre código de negocio y estado HTTP. |
| `src/main/java/co/reservas/application/service/reserva/CalculadoraDisponibilidad.java:57` | `filtrarDisponibles(...)` concentra el cálculo de cupos y recursos libres; lo reutilizan tanto la consulta de disponibilidad como las sugerencias de error. |
| `src/main/java/co/reservas/application/service/auth/SolicitadorVerificacion.java:25` | `solicitar(...)` solo emite el token y publica el evento; el envío del correo vive en otra clase. |

**Tensión conocida.** `src/main/java/co/reservas/application/service/reserva/ReservaService.java:60` lleva `@SuppressWarnings("java:S107")` porque el constructor recibe ocho dependencias (`ReservaService.java:61-72`). El caso de uso de crear reserva coordina cliente, servicio, agenda, recursos, reloj y verificador de propiedad; la supresión es una decisión consciente, no un descuido, pero es el punto del código donde SRP está más forzado. Si un jurado pregunta, la respuesta honesta es: la orquestación transaccional es intrínseca al caso de uso; extraerla a más colaboradores movería el problema sin reducirlo.

### 1.2 OCP — Abierto/cerrado

| Evidencia | Qué hace |
|-----------|----------|
| `src/main/java/co/reservas/adapters/out/persistence/servicio/CatalogoRepositoryJpaAdapter.java:42` | Las categorías de servicio se leen de la tabla, no de un `enum`: agregar una categoría nueva no toca código. |
| `src/main/java/co/reservas/infrastructure/security/SecurityConfig.java:124` | `PoliticaBloqueo` se construye desde propiedades (`max-intentos`, `duracion-bloqueo`); cambiar la política de bloqueo no modifica `IniciarSesionService`. |
| `src/main/java/co/reservas/application/port/out/auth/NotificacionPort.java:8` | Cambiar de SMTP a otro proveedor solo exige una implementación nueva del puerto; el caso de uso queda intacto. |

**Límite honesto.** `CodigosHttp.estado(...)` usa un `switch` exhaustivo sobre `CodigoError` (`CodigosHttp.java:15-29`): agregar un código nuevo obliga a editar esa clase. Es una decisión deliberada — el compilador falla si se olvida un caso — pero técnicamente es un punto cerrado a extensión y abierto a modificación.

### 1.3 LSP — Sustitución de Liskov

| Evidencia | Qué hace |
|-----------|----------|
| `src/test/java/co/reservas/soporte/RelojAjustable.java:12` | `RelojAjustable extends Clock` y sustituye al reloj real en pruebas sin que ningún servicio lo note, porque todos usan `Instant.now(clock)` (por ejemplo `ReservaService.java:89`). |
| `src/test/java/co/reservas/soporte/NotificacionesCapturadas.java:12` | Implementa `NotificacionPort` y se inyecta como `@Primary` (`src/test/java/co/reservas/soporte/ConfiguracionPruebas.java:31-34`); las pruebas de integración corren el flujo real de registro sin servidor de correo. |
| `src/main/java/co/reservas/adapters/out/security/BCryptPasswordHasherAdapter.java:32` | `coincide(...)` respeta el contrato de `PasswordHasherPort`: devuelve `false` ante entradas nulas o fuera de límite en lugar de lanzar, de modo que el llamador nunca necesita casos especiales. |

### 1.4 ISP — Segregación de interfaces

| Evidencia | Qué hace |
|-----------|----------|
| `src/main/java/co/reservas/application/port/in/agenda/CrearHorarioUseCase.java:7`, `EditarHorarioUseCase.java:5`, `EliminarHorarioUseCase.java:5`, `ConsultarHorariosUseCase.java:5` | Cuatro interfaces de un método cada una. `AgendaService` las implementa todas (`AgendaService.java:36-37`), pero `AgendaController` puede depender solo de las que usa. |
| `src/main/java/co/reservas/application/port/out/auth/PasswordHasherPort.java:3` | Dos métodos, sin nada más; el caso de uso de login no arrastra API de criptografía. |
| `src/main/java/co/reservas/application/port/in/reserva/CrearReservaUseCase.java:5` y `ConsultarReservasUseCase.java:7` | Separadas aunque las implementa la misma clase (`ReservaService.java:47`), lo que permite que un cliente de solo lectura dependa únicamente de la consulta. |

**Observación.** `ReservaController.java:36-38` inyecta tres puertos distintos en vez de un servicio gordo: la segregación se aprovecha realmente, no solo se declara.

### 1.5 DIP — Inversión de dependencias

| Evidencia | Qué hace |
|-----------|----------|
| `src/main/java/co/reservas/application/service/reserva/ReservaService.java:51-58` | Los ocho campos son interfaces (`*RepositoryPort`, `CalculadoraDisponibilidad`, `Clock`); ninguna clase de JPA ni de Spring Data entra al servicio. |
| `src/main/java/co/reservas/adapters/out/persistence/reserva/ReservaRepositoryJpaAdapter.java:19` | El adaptador implementa el puerto; la flecha de dependencia apunta de infraestructura hacia aplicación. |
| Verificación automatizable | `rg '^import co\.reservas\.(adapters\|infrastructure)' src/main/java/co/reservas/application` no devuelve resultados, y `rg '^import ' src/main/java/co/reservas/domain` solo devuelve `java.*` y `co.reservas.domain.*`. El dominio es Java puro y la aplicación no conoce adaptadores. |

**Brecha.** La regla de dependencia está documentada (`docs/modelo-datos-y-paquetes.md` §2.2) y hoy se cumple, pero **no hay una prueba que la haga fallar** si alguien la rompe (no existe ArchUnit ni equivalente en `src/test`). Es una convención vigilada por revisión humana, no por el build.

---

## 2. GRASP

| Patrón | Evidencia | Justificación |
|--------|-----------|---------------|
| **Experto en información** | `src/main/java/co/reservas/domain/agenda/HorarioDisponible.java:99` | `admiteTurno(...)` decide si un inicio cae en un turno válido usando solo los datos del propio bloque (`horaInicio`, `horaFin`, día). También `Usuario.estaBloqueado` (`Usuario.java:53`) y `TokenVerificacion.esValido` (`TokenVerificacion.java:41`). |
| **Creador** | `src/main/java/co/reservas/domain/reserva/Reserva.java:32` | `Reserva.crear(...)` construye la reserva porque recibe el `Servicio` que aporta duración y capacidad. `HistorialReserva.creacion(...)` (`HistorialReserva.java:11`) crea la fila de historial a partir de la reserva que ya existe. |
| **Controlador** | `src/main/java/co/reservas/adapters/in/web/reserva/ReservaController.java:67-70` | El método `crear` traduce JWT a `UsuarioAutenticado` y delega en el caso de uso; no contiene reglas. Lo mismo `AuthController.java:45-47`. |
| **Bajo acoplamiento** | `src/main/java/co/reservas/application/port/out/reserva/ReservaRepositoryPort.java:12` | El servicio de reservas conoce nueve operaciones expresadas en tipos de dominio (`Reserva`, `OcupacionRecurso`), no en entidades JPA. |
| **Alta cohesión** | `src/main/java/co/reservas/application/service/servicio/CatalogoService.java:13` | Tres métodos, un único tema. `CalculadoraDisponibilidad.java:22` concentra todo el cálculo de disponibilidad y nada más. |
| **Polimorfismo** | `src/test/java/co/reservas/soporte/ConfiguracionPruebas.java:25-34` | Los `@Primary` de reloj y notificaciones sustituyen implementaciones en tiempo de prueba; el código productivo no tiene ramas `if (esPrueba)`. |
| **Fabricación pura** | `src/main/java/co/reservas/application/service/reserva/ContextoAgenda.java:13` | No es un concepto del negocio: agrupa servicio, zona, bloques y recursos para no pasar cuatro parámetros. Igual `FabricaErrores.java:15` y `SesionActual.java:10`. |
| **Indirección** | `src/main/java/co/reservas/adapters/out/persistence/CatalogoSistema.java:17` | Traduce los `enum` de sistema a los ids de las filas semilla; ni el dominio conoce ids ni la base de datos conoce nombres de clases Java. |
| **Variaciones protegidas** | `src/main/java/co/reservas/adapters/in/web/ApiRutas.java:8` y `CodigosHttp.java:14` | El prefijo `/api/v1` y la traducción a HTTP están encapsulados: un cambio de versión o de estado HTTP no toca dominio ni aplicación. |

---

## 3. Patrones de diseño aplicados

Solo se listan los que están realmente en el código.

| Patrón | Dónde vive | Problema que resuelve aquí | Por qué valió la pena |
|--------|------------|----------------------------|------------------------|
| **Puertos y adaptadores (hexagonal)** | `application/port/in`, `application/port/out`, `adapters/in`, `adapters/out` | Aislar las reglas de reserva del transporte HTTP y de JPA | Permite probar las reglas de negocio sin contenedor y sustituir SMTP o reloj en pruebas (`ConfiguracionPruebas.java:25-34`) |
| **Repository** | `ReservaRepositoryPort.java:12` implementado por `ReservaRepositoryJpaAdapter.java:19` | Dar al caso de uso un vocabulario de dominio (`listarOcupaciones`, `contarConfirmadasEnTurno`) en lugar de SQL | El servicio expresa intención; los bloqueos y los JOIN quedan en el adaptador |
| **Adapter** | `BCryptPasswordHasherAdapter.java:12`, `JwtTokenEmisorAdapter.java:22`, `SmtpNotificacionAdapter.java:11` | Envolver APIs de terceros (Spring Security Crypto, Nimbus, JavaMail) detrás de puertos propios | Cambiar de BCrypt a Argon2 o de SMTP a una API HTTP es un archivo nuevo |
| **DTO + Mapper** | `RegistroRequest.aComando()` (`RegistroRequest.java:35`), `RecursoResultado.de(...)` (`port/in/recurso/RecursoResultado.java:7`), `ReservaService.mapear(...)` (`ReservaService.java:193`) | Impedir que el JSON de entrada y el modelo de persistencia se filtren al dominio | El contrato de la API puede evolucionar sin arrastrar el dominio |
| **Objeto de política (Policy)** | `PoliticaBloqueo.java:10`, creado como bean en `SecurityConfig.java:124` | Expresar «5 intentos, 15 minutos» como un objeto configurable y probable | `PoliticaBloqueoTest` prueba la regla sin levantar Spring; operaciones la ajusta por variable de entorno |
| **Servicio de dominio sin estado** | `GeneradorTurnos.java:16`, `DetectorReservasAfectadas.java:18`, `DetectorConflictosRecursos.java:13` | Reglas que no pertenecen a una sola entidad (generar turnos, detectar reservas afectadas, detectar conflictos de recurso) | Son clases `final` con constructor privado y métodos estáticos: puras, deterministas y baratas de probar |
| **Value Object** | `Turno.java:8`, `OcupacionRecurso.java:8`, `DetalleCampo.java:6`, `ElementoCatalogo.java:6` | Dar nombre e inmutabilidad a conceptos sin identidad | `record` de Java 21: igualdad por valor gratis y cero mutación compartida |
| **Método de fábrica estático** | `Usuario.registrar` (`Usuario.java:35`), `Servicio.nuevo` (`Servicio.java:32`), `Recurso.nuevo` (`Recurso.java:12`), `HorarioDisponible.nuevo` (`HorarioDisponible.java:30`), `TokenVerificacion.emitir` (`TokenVerificacion.java:22`) | Garantizar que un objeto nunca nazca inválido y normalizar entradas (trim, minúsculas) | Un `record` no puede existir en estado inconsistente; la validación no se puede saltar |
| **Fábrica (objeto)** | `FabricaErrores.java:15` | Construir el cuerpo de error uniforme con `traceId` y `timestamp` desde un único lugar | Controladores y filtros de seguridad producen exactamente el mismo JSON (`EscritorErrorSeguridad.java:33`) |
| **Inyección de dependencias por constructor** | Todo el código; ejemplo `AgendaService.java:47-54` | Dependencias explícitas y campos `final` | Ningún `@Autowired` sobre campos; los servicios se instancian a mano en pruebas unitarias |
| **Filtro / cadena de responsabilidad** | `TraceIdFilter.java:23` con `@Order(Ordered.HIGHEST_PRECEDENCE)` (`TraceIdFilter.java:22`), más la cadena de `SecurityConfig.java:46` | Poner el `traceId` en el MDC antes de seguridad para que los 401/403 también lo lleven | Trazabilidad extremo a extremo sin tocar ni un controlador |
| **Template Method (vía framework)** | `TraceIdFilter extends OncePerRequestFilter` y sobrescribe `doFilterInternal` (`TraceIdFilter.java:30`); `PruebaIntegracion` como base abstracta (`src/test/java/co/reservas/soporte/PruebaIntegracion.java:36`) | Reutilizar el esqueleto (una ejecución por petición; contexto + PostgreSQL + MockMvc) y solo escribir el paso variable | Las 6 clases de integración heredan arranque, helpers y limpieza de estado |
| **Evento de dominio / Observador** | `SolicitadorVerificacion.java:28` publica; `NotificadorVerificacionCuenta.java:24` escucha con `@TransactionalEventListener(AFTER_COMMIT)` | Enviar el correo **después** del commit, de modo que un fallo de SMTP no revierta el registro | Es la pieza que sostiene la regla «si el correo falla, el usuario existe y puede pedir reenvío» |
| **Traducción de excepciones** | `RestriccionesUnicas.java:19`, usado en `UsuarioRepositoryJpaAdapter.java:60` | Convertir la violación de `uk_usuarios_email` en `EMAIL_YA_REGISTRADO` cuando dos registros compiten | Cierra la carrera entre el `existeEmail` previo y el `insert`, sin filtrar excepciones de Hibernate hacia arriba |

**Patrones que NO están y no se reclaman.** No hay Builder propio (solo se consume el de `JwtClaimsSet` de Nimbus, `JwtTokenEmisorAdapter.java:43`), no hay Facade explícito, no hay Strategy con múltiples implementaciones intercambiables en runtime, no hay Decorator, Singleton propio (lo gestiona el contenedor) ni Null Object. Declararlos sería inflar la lista.

---

## 4. Otros principios

| Principio | Evidencia | Estado |
|-----------|-----------|--------|
| **Separación de intereses** | El dominio no importa nada fuera del JDK (verificado con `rg` sobre `src/main/java/co/reservas/domain`); las entidades JPA viven solo en `adapters.out.persistence` y se escanean desde `JpaConfig.java:11-12` | Cumple |
| **Regla de dependencia (arquitectura limpia)** | `rg '^import co\.reservas\.(adapters\|infrastructure)' src/main/java/co/reservas/application` → sin resultados | Cumple, pero sin prueba automática que lo proteja |
| **Validación fail-fast** | Bean Validation en el borde (`RegistroRequest.java:18-29`), invariantes en el constructor compacto (`HorarioDisponible.java:21-28`, `Reserva.java:22-30`), y validación de forma antes de tocar la base de datos (`RegistrarUsuarioService.java:49`) | Cumple |
| **Mínimo privilegio** | RBAC por endpoint y método HTTP (`SecurityConfig.java:55-74`); ABAC de propiedad (`VerificadorPropiedad.java:41-57`); contenedor con usuario sin privilegios (`Dockerfile:13,16`); secretos solo por variable de entorno (`application.yml:6-8,49`) | Cumple en la capa de aplicación; **falta MFA administrativo** |
| **Idempotencia donde aplica** | `RecursoService.asignarAServicio` reemplaza el conjunto completo de asignaciones (`RecursoService.java:77` → `RecursoRepositoryJpaAdapter.java:69-74`): repetir la misma llamada deja el mismo estado. El reenvío de verificación responde siempre `202` e invalida los tokens previos (`ReenviarVerificacionService.java:40`) | Cumple donde corresponde. `POST /reservas` **no es idempotente** y no hay clave de idempotencia: dos envíos del mismo formulario crean dos reservas si hay cupo |
| **Concurrencia defensiva** | Bloqueo pesimista en orden fijo cliente → servicio → recursos por id: `ReservaService.java:81`, `:83`, `:108`, con `@Lock(PESSIMISTIC_WRITE)` en `ClienteJpaRepository.java:15`, `ServicioJpaRepository.java:14` y `RecursoJpaRepository.java:24` (este último con `order by r.id`, `RecursoJpaRepository.java:15`). Verificado con una prueba que lanza reservas en paralelo por el último cupo: `src/test/java/co/reservas/adapters/in/web/reserva/ReservaIntegracionTest.java:185-186` | Cumple, con prueba real de concurrencia |
| **Fuente única de verdad del estado** | Los estados viven en `tbl_estados` (`V1__esquema_inicial.sql:22-27`) y se resuelven por nombre desde los `enum` en `CatalogoSistema.java:62-64`. El historial de transiciones se registra en la misma transacción (`ReservaService.java:114`) | Cumple |
| **Seguridad en logs** | Los eventos de seguridad registran `idUsuario`, nunca correo ni contraseña (`IniciarSesionService.java:102-107`); el evento de verificación sobrescribe `toString()` para no filtrar el token (`VerificacionCuentaSolicitada.java:8-11`) | Cumple |
| **Respuesta uniforme de error** | Un único cuerpo con `errorCode`, `message`, `details`, `traceId`, `timestamp` (`ErrorResponse.java:10-15`), producido tanto por `@RestControllerAdvice` (`GlobalExceptionHandler.java:97-99`) como por los manejadores 401/403 (`EscritorErrorSeguridad.java:29-34`) | Cumple |

**Hallazgo menor.** `Reserva.seSolapaCon(...)` (`Reserva.java:38`) solo se ejerce desde pruebas: en producción el solape del cliente se resuelve en SQL (`ReservaJpaRepository.java:21-24`). La regla está expresada dos veces, en dominio y en JPQL. No es un defecto funcional —el comportamiento coincide— pero es una duplicación que conviene declarar antes de que la pregunte un jurado.

---

## 5. Cumplimiento de la rúbrica y los lineamientos

Criterio de calificación en cada fila: `Cumple` = verificable hoy en el repositorio; `Parcial` = existe pero incompleto o sin evidencia publicada; `No cumple` = ausente.

### 5.1 Calidad interna (25 % de la nota) — lineamientos §3.5

| Requisito | Estado | Evidencia | Qué falta |
|-----------|--------|-----------|-----------|
| Análisis estático habilitado (SonarCloud/SonarQube) con Quality Gate activo | **No cumple** | No existe `sonar-project.properties`, ni plugin `sonar-maven-plugin` en `pom.xml`, ni proyecto enlazado en `docs/wiki.md` (la fila «Análisis de Calidad» está vacía) | Crear el proyecto en SonarCloud, ejecutar un análisis y enlazarlo |
| Cobertura unitaria ≥ 65 % (umbral de Sprint 1 para «Sobresaliente») | **Cumple** | Verificación JaCoCo obligatoria a 0,65 de líneas en `pom.xml:24` y `pom.xml:164-175`; `./mvnw verify` reporta 97,3 % total y 68,9 % contando solo pruebas unitarias | Nada, salvo publicarlo en Sonar |
| Deuda técnica ≤ 2 días | **No cumple** (no medible) | Métrica exclusiva de Sonar; sin instancia no hay dato | Exigible en Sprint 2 según la rúbrica |
| Complejidad ciclomática < 50 | **No cumple** (no medible) | Igual que la anterior | Exigible en Sprint 2 |
| Cero vulnerabilidades críticas / SCA | **No cumple** (no medible) | No hay SCA (Dependabot, OWASP Dependency-Check ni Snyk) configurado | Exigible en Sprint 2 |
| Pruebas unitarias con AAA | **Cumple** | Comentarios `// Arrange` / `// Act` / `// Assert` sistemáticos, p. ej. `src/test/java/co/reservas/application/service/recurso/RecursoServiceTest.java:67,72,75` | — |
| Pruebas de integración contra BD real o contenedor | **Cumple** | `PruebaIntegracion.java:32-35` levanta el contexto completo con PostgreSQL en Testcontainers; `ContextoAplicacionTest.java:22-30` valida el esquema migrado y las filas semilla | — |
| Pruebas de aceptación de backend | **Cumple** | Recorridos extremo a extremo por HTTP, p. ej. `ReservaIntegracionTest.java:70-71` (reserva exitosa con historial) y `RegistroUsuarioIntegracionTest.java:107` (fallo de correo con registro conservado) | — |
| Criterios de aceptación en Gherkin | **Parcial** | Los `@DisplayName` siguen la forma «Dado… cuando… entonces», p. ej. `ReservaIntegracionTest.java:185`, `ContextoAplicacionTest.java:21` | No hay archivos `.feature` ni Gherkin formal en Azure Boards enlazado desde el repositorio; la trazabilidad HU ↔ escenario vive en los nombres de prueba |

### 5.2 Arquitectura de solución (25 %) — lineamientos §3.1 y §4.4

| Requisito | Estado | Evidencia | Qué falta |
|-----------|--------|-----------|-----------|
| Estilo arquitectónico justificado y separación explícita | **Cumple** | Hexagonal con la regla de dependencia documentada en `docs/modelo-datos-y-paquetes.md` §2.2 y verificada en el código (sección 4 de este informe) | — |
| Módulos de dominio + módulo transversal | **Cumple** | `domain/{usuario,servicio,agenda,recurso,reserva}` y transversal de autenticación/autorización en `infrastructure/security` + `application/service/auth` | — |
| Mínimo 3 ADR priorizados | **Cumple desde hoy** | `docs/adr/0001` a `docs/adr/0005`, creados junto con esta auditoría | Antes de esta auditoría era **No cumple**; es el vacío que se cierra con el entregable 2 |
| Diagramas C4 o UML equivalente (paquetes, componentes, despliegue) | **Parcial** | Existen `docs/paquetes_&_componentes.json` y `docs/paquete_&_componentes_visio.vsdx`, pero `docs/modelo-datos-y-paquetes.md` los declara **superados y pendientes de actualización** | Regenerar el diagrama de paquetes/componentes con la estructura real y añadir un diagrama de despliegue (Render + Neon) |
| ERD / modelo lógico y físico | **Cumple** | ERD en Mermaid en `docs/modelo-datos-y-paquetes.md` §1.1 y modelo físico ejecutable en `V1__esquema_inicial.sql`; además `docs/entregable-bd/` con esquema, datos demo, consultas e imagen del ERD | `docs/entregable-bd/` está sin versionar en Git (aparece como `??` en `git status`) |
| Reglas de negocio más allá de CRUD | **Cumple** | Prevención de sobreocupación (`ReservaService.java:99`), conflicto de recursos con turno grupal compartido (`DetectorConflictosRecursos.java:36`), reservas afectadas al editar agenda (`DetectorReservasAfectadas.java:23`) | — |

### 5.3 API backend robusta — lineamientos §3.3

| Requisito | Estado | Evidencia | Qué falta |
|-----------|--------|-----------|-----------|
| REST versionado | **Cumple** | `ApiRutas.V1 = "/api/v1"` (`ApiRutas.java:8`), aplicado en todos los controladores (`ReservaController.java:32`, `AuthController.java:23`) | Política escrita de compatibilidad y retiro de versiones |
| Contrato OpenAPI/Swagger publicado | **Cumple** | `OpenApiConfig.java:16-27` con esquema `bearerAuth`; UI en `/swagger-ui.html` (`application.yml:64-66`); cada endpoint documenta sus errores (`ReservaController.java:63-66`) | — |
| Validación de payloads | **Cumple** | `@Valid` en los controladores (`AuthController.java:45`) sobre DTOs anotados (`RegistroRequest.java:18-29`) | — |
| Errores uniformes con `errorCode`, `message`, `details`, `traceId` y HTTP correcto | **Cumple** | `ErrorResponse.java:10-15`; mapa completo en `CodigosHttp.java:14-30`; también para 401/403 (`ManejadorNoAutenticado.java:36`, `ManejadorAccesoDenegado.java:39`) | — |
| Logs estructurados en JSON | **Cumple** | `logging.structured.format.console: ecs` fuera de `dev` y `test` (`application.yml:79-88`), con eventos con clave-valor (`IniciarSesionService.java:103-106`) | — |
| Endpoints de varios módulos | **Cumple** | auth, servicios, agenda, recursos, reservas, catálogos: 6 controladores en `adapters/in/web` | — |

### 5.4 Seguridad exigida — lineamientos §3.4

| Requisito | Estado | Evidencia | Qué falta |
|-----------|--------|-----------|-----------|
| RBAC por endpoint | **Cumple** | `SecurityConfig.java:55-74`: reglas por método HTTP y ruta, con `hasRole(PROVEEDOR)` / `hasRole(CLIENTE)` y `anyRequest().authenticated()` como cierre | — |
| ABAC simple por propiedad | **Cumple** | `VerificadorPropiedad.java:41-57` verifica existencia (404) y pertenencia (403) bloqueando la fila | — |
| MFA para accesos administrativos | **No cumple** | No hay segundo factor en el código; el rol `ADMIN` existe (`Rol.java:6-9`) pero no se autorregistra ni tiene endpoints propios | Declarado explícitamente fuera de alcance en `docs/modelo-datos-y-paquetes.md` §5 (Sprint 3) |
| Expiración de tokens | **Cumple** | JWT con `expiresAt` (`JwtTokenEmisorAdapter.java:48`), 30 min por defecto (`application.yml:50`), validado con `JwtTimestampValidator` y emisor (`SecurityConfig.java:117-119`) | — |
| Revocación y rotación de tokens | **No cumple** | No hay refresh token, lista de revocación ni rotación; el JWT es válido hasta expirar. Cada token lleva `jti` (`JwtTokenEmisorAdapter.java:44`), lo que deja preparada una futura lista negra | Declarado fuera de alcance (Sprint 3) |
| Bloqueo por intentos fallidos | **Cumple** | `PoliticaBloqueo.registrarIntentoFallido` (`PoliticaBloqueo.java:25-31`), aplicado en `IniciarSesionService.java:70` y confirmado con `@Transactional(noRollbackFor = ExcepcionNegocio.class)` (`IniciarSesionService.java:49`) | — |
| Política de contraseñas | **Cumple** | 12–64 caracteres (`RegistrarUsuarioService.java:82-84`) y límite de 72 bytes de BCrypt controlado (`BCryptPasswordHasherAdapter.java:39-41`) | — |
| Enumeración de usuarios evitada | **Cumple** | Hash ficticio en tiempo constante si el correo no existe (`IniciarSesionService.java:54`); reenvío siempre `202` (`AuthController.java:57-65`) | — |
| CORS restringido | **Cumple** | Solo orígenes de `APP_CORS_ALLOWED_ORIGINS`, sin comodín (`SecurityConfig.java:90-103`) | — |
| Consultas parametrizadas | **Cumple** | Todo el acceso es Spring Data con `@Param` (`ReservaJpaRepository.java:18-24`); no hay concatenación de SQL en el repositorio | — |
| Secretos por variable de entorno | **Cumple** | `application.yml:6-8,45,49`; `render.yaml:16-45` con `sync: false` y `generateValue` para `JWT_SECRET`; arranque abortado si el secreto es corto (`SecurityConfig.java:140-142`) | — |
| Eventos de seguridad registrados sin datos sensibles | **Cumple** | `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CUENTA_BLOQUEADA` con `idUsuario` (`IniciarSesionService.java:102-107`); `ACCESO_DENEGADO` con método y ruta (`ManejadorAccesoDenegado.java:33-38`) | — |
| SCA / gestión de vulnerabilidades | **No cumple** | Sin herramienta configurada | Exigible en Sprint 2 |

### 5.5 Persistencia y modelado — lineamientos §3.2 y §5

| Requisito | Estado | Evidencia | Qué falta |
|-----------|--------|-----------|-----------|
| Modelos lógico y físico normalizados | **Cumple** | `V1__esquema_inicial.sql` completo con FK, `CHECK` y `UNIQUE`; catálogos normalizados en tablas propias | — |
| Migraciones versionadas | **Cumple** | Flyway con `V1__esquema_inicial.sql` y `V2__datos_semilla.sql`; Hibernate solo valida (`application.yml:12`) | — |
| Índices justificados por consultas reales | **Cumple** | Cada índice lleva el comentario de la consulta que lo motiva: `V1__esquema_inicial.sql:60-61` (login), `:118-119` (generar turnos), `:165-168` (cupo y «mis reservas»), `:176-177` (conflictos de recurso) | Falta medir planes de ejecución con volumen representativo |
| Consultas no triviales | **Cumple** | Agregación por turno (`ReservaJpaRepository.java:14-19`) y JOIN con proyección a dominio (`ReservaRecursoJpaRepository.java:16-24`) | — |
| Auditoría y trazabilidad de cambios | **Cumple** | `tbl_historial_reservas` (`V1__esquema_inicial.sql:179-186`), escrita en la misma transacción (`ReservaService.java:114`) | — |
| Al menos un procedimiento almacenado o trigger | **No cumple** | No hay `CREATE FUNCTION`, `CREATE PROCEDURE` ni `CREATE TRIGGER` en `src/main/resources/db/migration` | Declarado como Sprint 3 en `docs/modelo-datos-y-paquetes.md` §5. Es un requisito de §3.2 con la salvedad «cuando sea coherente con la arquitectura» |

### 5.6 Entrega y despliegue — lineamientos §3.3 y §3.7

| Requisito | Estado | Evidencia | Qué falta |
|-----------|--------|-----------|-----------|
| Backend y BD contenerizados (Docker Compose) | **Cumple** | `docker-compose.yml` con `db`, `mailpit` y `app`, con healthcheck y dependencias ordenadas; `Dockerfile` multietapa con usuario sin privilegios | — |
| Despliegue en Render (o equivalente) | **Parcial** | `render.yaml:1-49` es un blueprint completo y listo; `README.md` §«Despliegue (Render + Neon)» documenta el procedimiento | **No hay evidencia en el repositorio de un despliegue ejecutado**: la fila «Entorno desplegado» de `docs/wiki.md` está vacía y no hay URL pública registrada |
| CI/CD con GitHub Actions | **No cumple** | No existe `.github/` en el repositorio | Los lineamientos §3.7 lo sitúan en Sprint 2 |
| PR con al menos una aprobación | **No verificable desde el repositorio** | La rama `feat/sprint1-backend-reservas` no ha sido integrada a `main` en este árbol | — |
| Al menos una HU de punta a punta (frontend–backend–BD–despliegue) | **Parcial** | Backend y BD completos y probados extremo a extremo por HTTP; **no hay frontend** en el repositorio | El perfil avanzado declara el frontend como N/A (lineamientos §1.3), pero `docs/wiki.md` pide una HU punta a punta incluyendo frontend y despliegue. Es una contradicción entre documentos que conviene declarar ante el jurado |

### 5.7 Cumplimiento funcional del sprint (30 %)

| HU | Estado | Evidencia |
|----|--------|-----------|
| HU-01 / HU-02 Registro de usuarios y proveedores | Cumple | `RegistrarUsuarioService.java:48` + `RegistroUsuarioIntegracionTest` |
| HU-03 Verificación e inicio de sesión | Cumple | `VerificarCuentaService.java:38`, `IniciarSesionService.java:50` + `InicioSesionIntegracionTest` |
| HU-04 Crear agenda | Cumple | `AgendaService.java:58` + `AgendaIntegracionTest` |
| HU-05 / HU-06 Editar y eliminar agenda con reservas afectadas | Cumple | `AgendaService.java:94` y `:116` |
| HU-07 Disponibilidad y creación de reservas | Cumple | `DisponibilidadService.java:47`, `ReservaService.java:80` |
| HU-09 Control de disponibilidad de recursos | Cumple | `ReservaService.java:146` + `DetectorConflictosRecursos.java:18` |
| HU-08 Cancelar reserva | Fuera de alcance declarado | `docs/modelo-datos-y-paquetes.md` §5 |
| Reportes de ocupación | Fuera de alcance declarado | `docs/modelo-datos-y-paquetes.md` §5 |

Sobre lo comprometido para el Sprint 1 (HU-01 a HU-07 y HU-09), la entrega es del 100 %, implementada, integrada y probada.

---

## 6. Brechas priorizadas

Ordenadas por peso en la calificación y, dentro de cada peso, por costo.

| # | Brecha | Criterio afectado | Esfuerzo | ¿Antes de la presentación? |
|---|--------|-------------------|----------|-----------------------------|
| 1 | **Sin SonarCloud ni Quality Gate activo** | Calidad interna (25 %) | 1–2 h: crear el proyecto, añadir `sonar-maven-plugin` o la acción, ejecutar un análisis con el informe JaCoCo ya existente | **Sí, y es la de mayor retorno.** La rúbrica exige explícitamente «análisis estático habilitado». La cobertura ya está en 68,9 % unitaria; solo falta publicarla |
| 2 | **Diagramas de paquetes/componentes desactualizados y sin diagrama de despliegue** | Arquitectura (25 %) | 2–3 h: regenerar a partir de `docs/modelo-datos-y-paquetes.md` §2.1 y dibujar Render + Neon + SMTP | **Sí.** Es entregable explícito de Sprint 1 (§3.7) y se defiende mejor con una imagen que con prosa |
| 3 | **Sin evidencia de despliegue ejecutado en Render** | Arquitectura + entrega | 1–2 h si Neon y Render se crean en el momento; el blueprint ya está escrito | **Sí, si hay tiempo.** «Despliegue inicial» es entregable de Sprint 1; sin URL viva la afirmación queda sin respaldo |
| 4 | **`docs/entregable-bd/` sin versionar** | Arquitectura + bases de datos | 5 min: `git add docs/entregable-bd` | **Sí, inmediato** |
| 5 | **Gherkin no formalizado fuera de los nombres de prueba** | Calidad interna | 2–4 h para escribir los escenarios de las HU priorizadas y enlazarlos a Azure Boards | Parcialmente: bastaría con exportar los `@DisplayName` ya escritos como escenarios |
| 6 | **Sin procedimiento almacenado ni trigger** | Persistencia (§3.2) | 2–3 h para un trigger de auditoría sobre `tbl_reservas` | Posible, pero **hay una razón arquitectónica para no hacerlo ahora**: el historial ya se escribe en la transacción del caso de uso y duplicarlo en un trigger crearía dos fuentes de verdad (§5.4 de los lineamientos lo desaconseja). Está planificado para Sprint 3 |
| 7 | **Sin CI/CD (GitHub Actions)** | Entrega | 1–2 h para un workflow de `mvnw verify` | Opcional: los lineamientos lo sitúan en Sprint 2 |
| 8 | **Sin MFA administrativo, ni revocación/rotación de tokens** | Seguridad (§3.4) | 1–2 días | **No antes de mañana.** Declarado como Sprint 3 en el diseño aprobado; defender esa planificación es mejor que improvisarlo |
| 9 | **Sin SCA / gestión de vulnerabilidades** | Seguridad + calidad | 1 h (Dependabot) | Opcional; exigible en Sprint 2 |
| 10 | **La regla de dependencia no está protegida por pruebas** | Arquitectura (mantenibilidad) | 1–2 h con ArchUnit | Opcional. Hoy se cumple; el riesgo es de regresión futura |
| 11 | **`POST /reservas` no es idempotente** | API (§4.2 «idempotencia cuando aplique») | 3–4 h con clave de idempotencia | No antes de mañana; conviene tener lista la respuesta (ver §7) |

---

## 7. Preguntas probables del jurado y cómo responderlas

**1. ¿Por qué arquitectura hexagonal y no una arquitectura por capas clásica?**
Porque el valor del sistema está en reglas que no dependen de la tecnología: generar turnos, detectar sobreocupación y detectar conflictos de recursos. Esas reglas viven en `domain` sin un solo import fuera del JDK, y por eso se prueban en milisegundos sin levantar Spring ni la base de datos. Una arquitectura por capas habría funcionado, pero habría dejado las reglas acopladas a JPA. El costo que pagamos es real —puertos, adaptadores y mapeo explícito— y lo asumimos conscientemente (ADR-0001).

**2. ¿Por qué los estados están en una tabla y no como `enum` en el esquema?**
Porque un `enum` de PostgreSQL obliga a `ALTER TYPE` para cada estado nuevo, y esa migración bloquea. Con `tbl_estados` un estado nuevo es un `INSERT`. Mantenemos, eso sí, el `enum` de Java como fuente de verdad de las reglas y lo sincronizamos por nombre con las filas semilla (`CatalogoSistema.java:62-64`). Así el código no manipula ids mágicos y la base de datos conserva integridad referencial (ADR-0002).

**3. ¿Cómo se evita la sobreocupación?**
Con bloqueo pesimista y orden de adquisición fijo. Al crear una reserva bloqueamos, en este orden, la fila del cliente, la del servicio y las de los recursos ordenadas por id (`ReservaService.java:81`, `:83`, `:108`). Con la fila del servicio bloqueada contamos las reservas confirmadas de ese turno y comparamos contra la capacidad (`ReservaService.java:99`). Tenemos una prueba que lanza varios clientes en paralelo por el último cupo y verifica que solo uno gana: `ReservaIntegracionTest.java:185-186`.

**4. ¿Por qué bloqueo pesimista y no una restricción única en la base de datos?**
Porque la regla no es «no repetir una fila»: es «no superar N reservas confirmadas en un turno», con N variable por servicio, y además «ningún recurso ocupado en un intervalo que se solape». Una restricción `UNIQUE` no expresa un conteo ni un solapamiento; una restricción de exclusión con `tstzrange` tampoco cubre el caso de la clase grupal, donde varias reservas comparten el mismo recurso en el mismo turno a propósito (`DetectorConflictosRecursos.java:36`). Sí usamos restricciones únicas donde sí aplican —correo, nombre de servicio, nombre de recurso— y traducimos su violación a error de negocio (`RestriccionesUnicas.java:19`) como última red ante carreras (ADR-0003).

**5. ¿Y el riesgo de interbloqueo?**
Se evita con el orden de adquisición: siempre cliente, después servicio, después recursos ordenados por id ascendente (`RecursoJpaRepository.java:15` incluye `order by r.id`). Dos transacciones que compiten por los mismos recursos los piden en la misma secuencia, así que una espera en lugar de cruzarse con la otra.

**6. ¿Qué pasa si el proveedor de correo falla durante el registro?**
El usuario queda registrado. El correo se envía después del commit mediante un evento (`SolicitadorVerificacion.java:28` publica, `NotificadorVerificacionCuenta.java:24` escucha en fase `AFTER_COMMIT`), y el fallo se captura y se registra como `CORREO_VERIFICACION_FALLIDO` sin propagarse (`NotificadorVerificacionCuenta.java:32-38`). El usuario puede pedir reenvío en `POST /auth/verificacion/reenvio`. Hay una prueba de integración que simula el fallo: `RegistroUsuarioIntegracionTest.java:107`.

**7. ¿Por qué la capacidad está en el servicio y no en el horario?**
Porque la capacidad es una propiedad del servicio, no del calendario: una consulta odontológica es capacidad 1 y una clase de spinning es capacidad 12, independientemente del día. Ponerla en el bloque de agenda obligaría a repetir el mismo número en cada bloque y abriría la puerta a inconsistencias. La duración del servicio cumple el mismo doble papel: es también la longitud del turno (`Servicio.java:11-19`, `GeneradorTurnos.java:40-49`).

**8. ¿Por qué no hay frontend?**
Porque el perfil avanzado define el frontend como N/A (lineamientos §1.3) y el entregable es un backend robusto integrado a base de datos. La verificación funcional se hace por Swagger UI (`/swagger-ui.html`), que sirve de interfaz de demostración, y por 187 pruebas automatizadas. Reconocemos que `docs/wiki.md` pide una HU punta a punta incluyendo frontend: es una tensión entre documentos, y aplicando la regla de precedencia (§1.2, «primero los criterios del nivel del equipo») prevalece el perfil avanzado.

**9. ¿Cómo se maneja la zona horaria?**
Cada proveedor declara su zona IANA (`Proveedor.java:31-37`) y la agenda se interpreta en ella (`GeneradorTurnos.java:21`). Todo lo que se persiste es `timestamptz` en UTC, y las respuestas devuelven `OffsetDateTime` en la zona del proveedor (`ReservaService.java:193-203`). El reloj es un bean inyectado (`ClockConfig.java:16`), así que «ahora» es sustituible en pruebas.

**10. ¿Cómo escalaría el sistema?**
La aplicación no guarda sesión: la autenticación es JWT con `SessionCreationPolicy.STATELESS` (`SecurityConfig.java:54`), así que se replica horizontalmente sin estado compartido. El límite lo pondría primero la base de datos, por los bloqueos pesimistas sobre la fila del servicio: reservas concurrentes del mismo servicio se serializan. Mitigaciones en orden de costo: índices ya presentes sobre `(id_servicio, fecha_hora_inicio)` (`V1__esquema_inicial.sql:166`), después caché de lectura para disponibilidad, y solo si hiciera falta, partición de reservas por rango de fecha. No introdujimos Redis ni Kafka porque no hay una necesidad demostrada, y los lineamientos §5.2 lo desaconsejan explícitamente.

**11. ¿Qué pasa si un cliente envía dos veces el mismo formulario de reserva?**
Se crean dos reservas si hay cupo: `POST /reservas` no es idempotente y hoy no hay clave de idempotencia. Lo que sí está protegido es el solape: un cliente no puede tener dos reservas confirmadas que se crucen en el tiempo (`ReservaService.java:104`), de modo que el duplicado exacto de un mismo turno sí se rechaza con `409 RESERVA_SOLAPADA`. La brecha real son dos reservas consecutivas no solapadas. La solución prevista es una cabecera `Idempotency-Key` con registro de la respuesta.

**12. ¿Por qué no hay procedimientos almacenados ni triggers, si los lineamientos los piden?**
Porque los lineamientos los piden «cuando sea coherente con la distribución de responsabilidades de la arquitectura» (§3.2) y advierten en §5.4 contra duplicar reglas sin una fuente de verdad definida. El historial de reservas ya se escribe dentro de la misma transacción del caso de uso (`ReservaService.java:114`); añadir un trigger que hiciera lo mismo crearía dos fuentes de verdad para el mismo hecho. Está planificado para Sprint 3, donde los lineamientos lo sitúan junto al refinamiento del MER.

**13. ¿Cómo se garantiza que nadie vea o modifique datos de otro proveedor?**
Con dos capas. La primera es RBAC por endpoint y método HTTP (`SecurityConfig.java:55-74`). La segunda es ABAC de propiedad: `VerificadorPropiedad` carga el servicio, carga el proveedor del usuario autenticado y compara (`VerificadorPropiedad.java:49-57`), devolviendo 404 si no existe y 403 si existe pero no le pertenece. Esa distinción es intencional: no revela la existencia de recursos ajenos más allá de lo que ya revela el catálogo público.

**14. ¿Por qué el `traceId` y qué se hace con él?**
Cada petición entra por `TraceIdFilter` (`TraceIdFilter.java:23`), que se ejecuta con precedencia máxima (`:22`) para que incluso los 401 y 403 lo lleven. El filtro acepta un `X-Trace-Id` entrante si cumple un formato seguro (`:43`) o genera uno, lo publica en el MDC —de ahí pasa a los logs JSON— y lo devuelve en la respuesta. Todos los cuerpos de error lo incluyen (`FabricaErrores.java:26`), así que un usuario puede reportar un error y el equipo encuentra exactamente esa petición en los logs.
