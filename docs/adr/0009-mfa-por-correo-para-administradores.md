# ADR-0009 — MFA por correo para administradores, con aprovisionamiento por variables de entorno

- **Estado:** Propuesta
- **Fecha:** 24 de noviembre de 2026
- **Responsable:** Equipo Avanzado — Sara Mesa
- **Ámbito:** Seguridad, autenticación de cuentas privilegiadas
- **Relación:** completa [ADR-0004](0004-autenticacion-jwt-sin-estado-y-bloqueo-por-intentos-fallidos.md) y [ADR-0006](0006-sesiones-revocables-con-tokens-de-refresco-rotativos.md), que no se reemplazan

## Contexto

Los lineamientos §3.4 exigen *«MFA para acceso administrativo»*. El Sprint 3 dejó la base: `tbl_codigos_mfa` (migración `V6`), el dominio `CodigoMfa` y `MfaAdminService`. Una revisión de cierre encontró que el MFA **no se podía usar**:

1. **Sin endpoints.** Ningún controlador invocaba `MfaAdminUseCase`.
2. **El código nunca se enviaba.** `NotificacionPort.enviarCodigoMfa` tenía una implementación vacía por defecto y el adaptador SMTP no la sobrescribía.
3. **Se podía saltar.** `POST /auth/login` no miraba el rol: una cuenta `ADMIN` entraba solo con su contraseña.
4. **Sin protección contra fuerza bruta.** Un código de 6 dígitos tiene un millón de combinaciones y la verificación no limitaba los intentos. Además, cada solicitud dejaba vigentes los códigos anteriores, así que pedir varios multiplicaba la probabilidad de acertar. La solicitud tampoco sumaba los errores de contraseña al bloqueo, así que el endpoint MFA era un atajo para evitar el bloqueo del login.
5. **No había cómo crear un administrador.** El registro público rechaza `ADMIN` (correcto) y ninguna migración crea uno.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **TOTP (aplicación autenticadora, RFC 6238)** | Es más fuerte que el correo y no depende del SMTP. Pero exige enrolar el secreto (código QR), guardarlo cifrado, códigos de recuperación y una tabla nueva, y no reutiliza nada de lo ya construido. Queda como evolución. |
| **Contador de intentos propio en `tbl_codigos_mfa`** | Requiere una migración y deja dos políticas de bloqueo que se pueden desincronizar. Reutilizar `PoliticaBloqueo` da una sola regla para todas las vías de entrada. |
| **Administrador inicial en una migración Flyway** | La contraseña (o su hash) quedaría en el repositorio y sería la misma en todos los entornos. |
| **Endpoint para crear administradores** | Requiere que ya exista un administrador (problema del huevo y la gallina) o abre una vía de escalamiento de privilegios. |

## Decisión

- **Dos pasos públicos:** `POST /api/v1/auth/mfa/solicitud` (correo y contraseña → `202` y código por correo) y `POST /api/v1/auth/mfa/verificacion` (correo y código → la misma respuesta que el login, con `accessToken` y `refreshToken`). El código tiene 6 dígitos de `SecureRandom`, se guarda como hash SHA-256, vence a los 5 minutos y sirve una vez. Pedir un código nuevo anula los anteriores, así que solo vale el último. Con un correo inexistente se compara contra un hash ficticio, como en el login, para que el tiempo de respuesta no revele qué cuentas existen.
- **`/auth/login` rechaza las cuentas `ADMIN`** con `403 ACCESO_DENEGADO`, pero solo después de validar la contraseña, para no revelar qué correos son de administrador.
- **Un solo contador de bloqueo.** Las contraseñas y los códigos incorrectos suman al contador de `PoliticaBloqueo`: 5 errores bloquean la cuenta 15 minutos (`423`). El contador solo se reinicia al completar el segundo paso, así que pedir un código nuevo no da más intentos. Las transacciones usan `noRollbackFor = ExcepcionNegocio.class` para que los intentos se guarden aunque la respuesta sea un error.
- **El correo se envía de verdad:** `SmtpNotificacionAdapter.enviarCodigoMfa`.
- **Aprovisionamiento por configuración:** si se definen `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` (opcional `APP_ADMIN_NOMBRE`), `AdministradorInicialConfig` crea la cuenta al arrancar a través de `AprovisionarAdministradorUseCase`. Es idempotente: si el correo existe no cambia nada. Un valor inválido detiene el arranque en lugar de crear una cuenta débil.

## Consecuencias

**Positivas**

- Se cumple el requisito de §3.4 de punta a punta, con pruebas de integración (`MfaIntegracionTest`), unitarias y un escenario Gherkin (`mfa_administrador.feature`).
- Sin cambios de esquema: reutiliza `tbl_codigos_mfa` y las columnas de bloqueo de `tbl_usuarios`.
- Los intentos fallidos aparecen en las métricas (`codigo_error="CREDENCIALES_INVALIDAS"` sobre `/auth/mfa/*`) y disparan las alertas de seguridad del ADR-0008.

**Negativas**

- El segundo factor depende del correo: si el buzón del administrador está comprometido, el MFA no protege. TOTP lo resolvería.
- Si el SMTP falla, el administrador no puede entrar. Es aceptable para una sola cuenta de operación.
- La cuenta `ADMIN` todavía no tiene funcionalidades propias (gestión de usuarios, auditoría): hoy solo inicia sesión y gestiona su sesión.
- `APP_ADMIN_PASSWORD` queda en las variables del entorno del despliegue. Conviene cambiarla después del primer arranque o quitarla, porque la cuenta ya existe.

## Fuera de alcance

- TOTP o llaves de seguridad (WebAuthn).
- Endpoints de administración.
