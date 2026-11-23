# ADR-0006 — Sesiones revocables con tokens de refresco rotativos y detección de reutilización

- **Estado:** Aceptada
- **Fecha:** 23 de noviembre de 2026
- **Responsable:** Equipo Avanzado — Isaac Mesa
- **Ámbito:** Seguridad, gestión de sesiones, revocación de credenciales
- **Relación:** completa [ADR-0004](0004-autenticacion-jwt-sin-estado-y-bloqueo-por-intentos-fallidos.md), que no se reemplaza

## Contexto

El [ADR-0004](0004-autenticacion-jwt-sin-estado-y-bloqueo-por-intentos-fallidos.md) dejó escrita su propia deuda. En «Consecuencias negativas» la enunció sin rodeos: *«un token válido no se puede revocar antes de su expiración. Es la consecuencia directa de la ausencia de estado, y es la brecha de seguridad más relevante del sprint»*. Y en «Fuera de alcance» planificó la solución para el Sprint 3: *«refresh tokens y revocación/rotación quedan planificados para el Sprint 3»*.

Los lineamientos §3.4 exigen las tres cosas juntas: expiración, revocación y rotación segura de tokens. Con solo un token de acceso de 30 minutos se cumplía la primera y ninguna de las otras dos. Cerrar sesión no cerraba nada: el token seguía siendo válido hasta expirar.

Hay una restricción que acota el diseño. El objetivo de escalabilidad de los lineamientos §4.3 pide componentes sin estado cuando sea viable, y el ADR-0004 lo tomó como criterio central. Cualquier mecanismo de revocación introduce estado por definición; la pregunta de diseño no era si introducirlo, sino **dónde ponerlo para que no caiga en el camino crítico de cada petición**.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **Acortar la vigencia del token de acceso a 5 minutos** | No es revocación, es una ventana más chica. Obliga a reautenticar con contraseña cada cinco minutos, lo que empuja a los clientes a guardar la contraseña. Empeora la seguridad real mientras mejora la métrica. |
| **Lista de revocación por `jti` consultada en cada petición** | El ADR-0004 ya previó el `jti` para esto. Se descartó porque obliga a una consulta por petición, que es exactamente el estado en el camino crítico que la decisión anterior quería evitar. Reintroduce en todas las peticiones el costo que el refresco concentra en una cada 30 minutos. |
| **Tokens opacos con almacén en base de datos** | Ya se había descartado en el ADR-0004 por el mismo motivo: una consulta por petición. No aparecieron razones nuevas para revisarlo. |
| **Token de refresco de larga vida y un solo uso, sin rotación** | Da revocación pero no rotación, que los lineamientos piden explícitamente. Un token robado sirve hasta que caduca y el robo no deja rastro. |
| **Guardar el token de refresco en una cookie `HttpOnly`** | Es la recomendación habitual para aplicaciones de navegador y evita que el token quede accesible a JavaScript. Se descartó porque introduciría cookies en una API que hoy no las usa, y con cookies **sí aparecería un vector CSRF**, obligando a reactivar la protección y a un diseño de doble envío. El cliente de este proyecto es programático y guarda el token él mismo. |

## Decisión

### Token de refresco

Se adopta un **token de refresco opaco, persistido como hash y de un solo uso con rotación**.

- **Generación:** 32 bytes de `SecureRandom`, codificados en Base64 URL sin relleno. No es un JWT y no transporta reivindicaciones: es un identificador sin significado.
- **Persistencia:** nunca se guarda en claro. Se almacena el **hash SHA-256** en `tbl_tokens_refresco` (migración `V5__tokens_refresco.sql`), con la misma función que ya usaban los tokens de verificación de cuenta. Una filtración de la base no entrega credenciales utilizables. La restricción `uk_tokens_refresco_hash` hace doble trabajo: garantiza unicidad y da el índice con el que se busca el token en cada refresco; `ix_tokens_refresco_usuario` sostiene la revocación por usuario.
- **Vigencia:** 7 días, frente a los 30 minutos del token de acceso.
- **Rotación:** `POST /api/v1/auth/refresh` revoca el token presentado y emite un par nuevo. Un token de refresco sirve **exactamente una vez**.
- **Revocación explícita:** `POST /api/v1/auth/logout` revoca el token de refresco. Cerrar sesión ahora cierra la sesión.

### Detección de reutilización

Si llega un token de refresco que **ya estaba revocado**, no se responde solamente con un error: se **revocan todos los tokens activos de ese usuario** y se registra el evento `REUTILIZACION_TOKEN_REFRESCO`.

El razonamiento es que un token de un solo uso presentado dos veces tiene dos explicaciones: o el cliente legítimo reintentó, o alguien copió el token. No se puede distinguir una de la otra, y la asimetría de costos es clara: forzar a reautenticar a un usuario legítimo es una molestia, dejar viva una sesión robada no. Ante la duda, cae toda la familia.

### Dónde queda el estado

El estado de sesión existe ahora, pero **solo en la ruta de refresco**. Las peticiones a los endpoints de negocio siguen validando el token de acceso por firma, sin tocar la base de datos. El ADR-0004 no se reemplaza: su modelo sigue describiendo el 100 % del tráfico de negocio.

### CSRF: la decisión del ADR-0004 se mantiene

El ADR-0004 deshabilitó CSRF «por tratarse de una API sin estado y sin cookies de sesión». **Esa decisión se reafirma y la razón sigue intacta**, porque el token de refresco viaja en el **cuerpo JSON** de la petición, no en una cookie:

```
POST /api/v1/auth/refresh
{ "refreshToken": "..." }
```

Un ataque CSRF se apoya en que el navegador adjunte la credencial automáticamente. Ninguna credencial de esta API se adjunta de forma automática: ni el token de acceso, que va en la cabecera `Authorization`, ni el de refresco, que va en el cuerpo. Un sitio de terceros puede inducir una petición, pero no puede hacer que lleve credenciales que él no conoce.

Esto queda escrito acá porque el análisis estático marca `SecurityConfig.java` con `java:S4502` y la regla no puede saber si la aplicación usa cookies. El hallazgo es correcto como pregunta y la respuesta es esta. Si en el futuro se adoptara autenticación por cookie, **esta decisión queda invalidada** y habría que reactivar la protección.

### MFA para administradores: implementado, no expuesto

Hay que ser preciso sobre el estado real, porque los lineamientos §3.4 lo exigen y todavía no se cumple.

Lo que existe: `MfaAdminService` con códigos de 5 minutos de vigencia y la tabla `tbl_codigos_mfa` (migración `V6__codigos_mfa.sql`), con sus operaciones de solicitud y verificación en la capa de aplicación.

Lo que **no** existe: ningún controlador llama a `MfaAdminUseCase`. No hay endpoint, así que la funcionalidad **no es alcanzable** y no puede considerarse entregada. El rol `ADMIN` tampoco tiene endpoints propios, de modo que hoy no hay un acceso administrativo que proteger. Queda como pendiente declarado y no como requisito cumplido.

## Consecuencias

### Positivas

- **Se cierra la brecha que el ADR-0004 declaró como la más relevante.** Las sesiones son revocables y cerrar sesión surte efecto.
- Se cumplen las tres exigencias de los lineamientos §3.4 sobre tokens: expiración, revocación y rotación.
- El robo de un token de refresco es **detectable**, no solo posible de mitigar: la segunda presentación del mismo token delata la copia y corta todas las sesiones del usuario.
- El token de refresco guardado como hash hace que una filtración de la base no entregue credenciales usables, igual que ya ocurría con las contraseñas y los tokens de verificación.
- El costo de la revocación se paga una vez cada 30 minutos, no en cada petición.

### Negativas

- **Ya hay estado de sesión en la base de datos.** El objetivo «sin estado» del ADR-0004 deja de ser absoluto. Se acota al endpoint de refresco, pero la afirmación «la aplicación no guarda estado de sesión» ya no es cierta sin matizarla, y así hay que decirla.
- **El token de acceso sigue siendo irrevocable durante su vigencia.** Revocar el refresco impide renovar, no invalida el token de acceso que ya está en circulación: queda una ventana de hasta 30 minutos. Cerrarla exigiría la lista de revocación por `jti` que se descartó arriba.
- La tabla de tokens de refresco **crece y nadie la limpia**. Las dos consultas están indexadas, así que no se degrada la latencia, pero cada inicio de sesión y cada rotación dejan una fila que nunca se borra. Falta una purga de los expirados, que no existe todavía.
- La detección de reutilización puede **cerrar sesiones legítimas** si un cliente reintenta un refresco tras un error de red. Es una decisión consciente y el evento queda registrado para poder diagnosticarlo.
- MFA sigue sin cumplirse, ahora con código escrito pero inalcanzable, que es una forma de deuda más engañosa que no tener nada: parece hecho en el repositorio y no funciona.
