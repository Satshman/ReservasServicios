# Plataforma de Reservas de Servicios

Backend REST para gestionar reservas de servicios en cualquier tipo de negocio (clínicas, peluquerías, gimnasios, academias). Cada establecimiento se registra como proveedor y configura sus servicios, su agenda semanal y los recursos que comparten sus servicios. Los clientes consultan turnos disponibles y reservan sin sobreocupación.

Proyecto académico de CodeF@ctory UdeA 2026-2, perfil de equipo avanzado. El diseño aprobado (modelo de datos, paquetes, endpoints, contrato de errores y reglas de negocio) está en [`docs/modelo-datos-y-paquetes.md`](docs/modelo-datos-y-paquetes.md).

## Alcance del Sprint 1

| HU | Funcionalidad |
|----|---------------|
| HU-01, HU-02 | Registro de clientes y de proveedores con sus servicios |
| HU-03 | Verificación de cuenta por correo, inicio de sesión con JWT y bloqueo por intentos fallidos |
| HU-04, HU-05, HU-06 | Crear, editar y eliminar bloques de agenda, con confirmación cuando afectan reservas |
| HU-07 | Consultar disponibilidad y crear reservas |
| HU-09 | Recursos compartidos entre servicios sin conflictos de uso |

## Tecnologías

Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Security con OAuth2 Resource Server, Validation, Mail, Actuator) · PostgreSQL 17 · Flyway · springdoc-openapi · JUnit 5, Mockito, Testcontainers · JaCoCo · Docker/Podman Compose.

Arquitectura hexagonal en `co.reservas`: `domain` (Java puro), `application` (casos de uso y puertos), `adapters` (web, persistencia JPA, seguridad y correo) e `infrastructure` (arranque y configuración).

## Requisitos

- JDK 21 (`JAVA_HOME` apuntando a él). No se necesita Maven: se usa el wrapper `./mvnw`.
- Docker con Compose, o Podman con `podman-compose`.

## Ejecutar con Compose

```bash
cp .env.example .env        # ajuste JWT_SECRET y, si quiere, los puertos
podman-compose up --build   # o: docker compose up --build
```

| Servicio | URL |
|----------|-----|
| API | http://localhost:8081/api/v1 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Mailpit (correos enviados) | http://localhost:8025 |
| PostgreSQL | `localhost:5433`, base `reservas` |

Los puertos del host se cambian con `APP_HOST_PORT`, `DB_HOST_PORT`, `MAILPIT_SMTP_PORT` y `MAILPIT_UI_PORT`. Para detener: `podman-compose down` (agregue `-v` para borrar los datos).

## Ejecutar localmente (perfil `dev`)

Levante solo la base de datos y Mailpit, y ejecute la aplicación desde el código:

```bash
podman-compose up -d db mailpit
PORT=8082 APP_BASE_URL=http://localhost:8082 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

El perfil `dev` trae valores locales (base en `localhost:5433`, SMTP en `localhost:1025`, un secreto JWT solo para desarrollo) y logs legibles. Sin perfil, los logs salen en JSON (formato ECS) y toda la configuración se toma de variables de entorno (ver `.env.example`).

## Pruebas

Las pruebas de integración usan Testcontainers con PostgreSQL real, así que necesitan un motor de contenedores. Con Podman sin root, exporte antes:

```bash
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

| Tarea | Comando |
|-------|---------|
| Todas las pruebas y verificación de cobertura (≥ 65 % de líneas) | `./mvnw verify` |
| Solo ejecutar pruebas | `./mvnw test` |
| Una clase de prueba | `./mvnw test -Dtest=ReservaIntegracionTest` |
| Un método de prueba | `./mvnw test -Dtest=IniciarSesionServiceTest#cuentaBloqueada` |

El reporte de cobertura queda en `target/site/jacoco/index.html`. Las pruebas siguen el patrón AAA y sus nombres (`@DisplayName`) describen el escenario Gherkin de cada criterio de aceptación. Las clases `*IntegracionTest` recorren la API con MockMvc; el resto son pruebas unitarias del dominio, los servicios de aplicación y los adaptadores.

## Contrato de la API

- Versionado por ruta: `/api/v1`. Contrato OpenAPI en `/v3/api-docs`.
- Autenticación: `Authorization: Bearer <accessToken>` obtenido en `POST /api/v1/auth/login` (JWT HS256, 30 minutos).
- Todo error, incluidos 401 y 403, usa el mismo cuerpo:

```json
{ "errorCode": "TURNO_SIN_CUPO", "message": "…", "details": [], "traceId": "…", "timestamp": "2026-10-05T13:00:00Z" }
```

- Cada respuesta incluye la cabecera `X-Trace-Id` (se respeta la recibida si es válida) para correlacionar con los logs.

## Recorrido por las historias en Swagger

Abra http://localhost:8081/swagger-ui.html con el stack de Compose en marcha.

1. **Catálogos**: `GET /catalogos/categorias` y `GET /catalogos/tipos-recurso` para conocer los ids.
2. **HU-02**: `POST /auth/registro` con `rol: PROVEEDOR`, una `zonaHoraria` (por ejemplo `America/Bogota`) y al menos un servicio. Sin servicios responde `400 SERVICIO_REQUERIDO`.
3. **HU-03**: abra Mailpit (http://localhost:8025), copie el enlace del correo y ábralo, o llame `GET /auth/verificacion?token=…`. Luego `POST /auth/login`; copie `accessToken` y péguelo en **Authorize**.
4. **HU-04**: `GET /servicios?idProveedor=…` para obtener el id del servicio y `POST /servicios/{idServicio}/horarios` con `{"diasSemana":[1,2,3,4,5],"horaInicio":"08:00","horaFin":"12:00"}`.
5. **HU-09**: `POST /recursos` (por ejemplo, "Consultorio 1") y `PUT /servicios/{idServicio}/recursos` con `{"idsRecursos":[id]}`.
6. **HU-01 y HU-07**: registre y verifique un cliente (`rol: CLIENTE`), inicie sesión y autorice con su token. Consulte `GET /servicios/{idServicio}/disponibilidad?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` y reserve un turno con `POST /reservas`. Repetir el mismo turno con otro cliente produce `409 TURNO_SIN_CUPO` con sugerencias; una hora fuera de la agenda produce `422 HORARIO_NO_DISPONIBLE`.
7. **HU-05 y HU-06**: con el token del proveedor, `PUT /horarios/{id}` reduciendo el bloque para que excluya la reserva devuelve `409 HORARIO_CON_RESERVAS`; repita con `confirmar=true` para aplicarlo. `DELETE /horarios/{id}` funciona igual.
8. Cinco contraseñas incorrectas seguidas bloquean la cuenta 15 minutos (`423 CUENTA_BLOQUEADA` aunque la contraseña sea correcta).

## Configuración

| Variable | Uso | Valor por defecto |
|----------|-----|-------------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión JDBC a PostgreSQL | obligatorias |
| `JWT_SECRET` | Secreto HS256 (mínimo 32 bytes) | obligatoria |
| `JWT_EXPIRATION` | Vigencia del token de acceso | `30m` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Servidor SMTP | `MAIL_HOST` obligatoria, puerto `587` |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `APP_MAIL_FROM` | Opciones de SMTP y remitente | `false`, `false`, `no-reply@reservas.local` |
| `APP_BASE_URL` | Base del enlace de verificación | obligatoria |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos, separados por coma | ninguno |
| `LOGIN_MAX_ATTEMPTS`, `LOGIN_LOCK_DURATION` | Política de bloqueo | `5`, `15m` |
| `PORT`, `SWAGGER_ENABLED` | Puerto HTTP y publicación de Swagger | `8080`, `true` |
