# ADR-0004 — Autenticación con JWT sin estado y bloqueo temporal por intentos fallidos

- **Estado:** Aceptada
- **Fecha:** 17 de septiembre de 2026
- **Responsable:** Equipo Avanzado — Isaac Mesa
- **Complementada por:** [ADR-0006](0006-sesiones-revocables-con-tokens-de-refresco-rotativos.md) (23 de noviembre de 2026), que resuelve la consecuencia negativa sobre revocación de tokens y detalla el estado real del MFA. La decisión de este ADR sigue vigente y describe el tráfico de negocio.

## Contexto

Los lineamientos §3.4 exigen RBAC por endpoint, reglas ABAC simples cuando la propiedad determina el acceso, políticas de contraseña seguras, bloqueo ante intentos fallidos, expiración de tokens y registro de eventos de seguridad sin datos sensibles. Los requisitos no funcionales de `docs/wiki.md` añaden MFA para accesos administrativos.

El despliegue objetivo es un contenedor en Render con posibilidad de réplicas, y el atributo de escalabilidad de los lineamientos §4.3 pide componentes sin estado cuando sea viable. No hay un proveedor de identidad institucional disponible para el proyecto, ni presupuesto operativo para uno.

La API es consumida por clientes programáticos (Swagger UI en el Sprint 1) y no por un navegador con sesión, de modo que no hay un flujo de formulario ni necesidad de cookies.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| **Sesiones de servidor con `JSESSIONID`** | Obliga a estado compartido entre réplicas (sticky sessions o almacén de sesiones externo), justo lo contrario del objetivo de escalabilidad horizontal. Añade una pieza de infraestructura sin aportar al alcance evaluado. |
| **Proveedor de identidad externo con OIDC/OAuth 2.0 (Keycloak, Auth0)** | Es lo que recomiendan los lineamientos «cuando se integre un proveedor de identidad autorizado». No hay uno disponible ni autorizado para el curso, y autohospedar Keycloak implicaría un contenedor más, su base de datos y su operación, desproporcionado para el Sprint 1. Queda como evolución natural. |
| **JWT firmado con RS256 y par de claves** | Preferible cuando varios servicios verifican tokens emitidos por un tercero. Aquí el emisor y el verificador son el mismo proceso, así que la gestión de un par de claves añade operación (rotación, distribución de la clave pública) sin beneficio actual. |
| **Tokens opacos con almacén en base de datos** | Permitiría revocación inmediata, pero obliga a una consulta por petición, reintroduciendo estado en el camino crítico y contradiciendo el objetivo sin estado. |
| **Bloqueo permanente de la cuenta tras N intentos** | Convierte un ataque de fuerza bruta en una negación de servicio contra el usuario legítimo. Los lineamientos piden bloqueo, no bloqueo irreversible. |

## Decisión

### Autenticación

Se adopta **JWT de acceso firmado con HS256**, emitido por la propia aplicación y verificado como *resource server* de Spring Security, con `SessionCreationPolicy.STATELESS`. El token lleva `sub` con el identificador de usuario, la reivindicación `rol`, `iss`, `iat`, `exp` y un `jti` único. La vigencia por defecto es de 30 minutos y es configurable por variable de entorno.

El secreto de firma proviene exclusivamente del entorno (`JWT_SECRET`) y la aplicación **aborta el arranque** si tiene menos de 32 bytes. La validación del token comprueba la firma, la vigencia con una tolerancia de 30 segundos y el emisor esperado. El `jti` se incluye desde ahora para que una lista de revocación futura no exija cambiar el formato del token.

### Autorización

RBAC declarativo por método HTTP y ruta en la cadena de seguridad, con `anyRequest().authenticated()` como cierre por defecto, complementado con ABAC de propiedad en la capa de aplicación: un proveedor solo opera sobre sus propios servicios y recursos, distinguiendo `404` (no existe) de `403` (existe pero no le pertenece).

CORS restringido a los orígenes declarados en `APP_CORS_ALLOWED_ORIGINS`, sin comodín. CSRF deshabilitado por tratarse de una API sin estado y sin cookies de sesión.

### Contraseñas y bloqueo

Hash con BCrypt, con la longitud máxima de 72 bytes controlada explícitamente. Política de contraseñas de 12 a 64 caracteres sin reglas de composición, siguiendo la recomendación de los lineamientos de no imponer cambios ni composición arbitraria.

Bloqueo temporal parametrizable, por defecto **5 intentos fallidos y 15 minutos de bloqueo**, expresado como un objeto de política de dominio y construido como bean desde la configuración. Durante el bloqueo no se acumulan intentos adicionales.

Contramedidas de enumeración: si el correo no existe se ejecuta igualmente una comparación de hash contra un valor ficticio para mantener el tiempo de respuesta, y el reenvío de verificación responde siempre `202` exista o no la cuenta. El error `423 CUENTA_BLOQUEADA` solo se revela cuando la contraseña es correcta; en caso contrario se responde `401`, para no confirmar la existencia de la cuenta a quien no conoce la clave.

Los eventos `LOGIN_EXITOSO`, `LOGIN_FALLIDO` y `CUENTA_BLOQUEADA` se registran con el identificador de usuario, nunca con el correo ni la contraseña. La transacción de inicio de sesión se marca para **no revertir** ante un error de negocio, de modo que el incremento de intentos fallidos persiste aunque la respuesta sea un error.

### Fuera de alcance de esta decisión

MFA para accesos administrativos, refresh tokens y revocación/rotación quedan planificados para el Sprint 3, coherente con el entregable «aseguramiento mediante tokens» de los lineamientos §3.7.

## Consecuencias

### Positivas

- La aplicación no guarda estado de sesión, por lo que se replica horizontalmente sin infraestructura adicional.
- La autorización por rol es declarativa y legible en un solo archivo, lo que la hace auditable endpoint por endpoint.
- El bloqueo temporal y las contramedidas de enumeración cubren los controles que los lineamientos exigen explícitamente, con pruebas unitarias y de integración que los ejercen.
- Los secretos nunca están en el repositorio y el arranque falla de forma ruidosa si falta el secreto o es débil, en lugar de operar de forma insegura.

### Negativas

- **Un token válido no se puede revocar antes de su expiración.** Es la consecuencia directa de la ausencia de estado, y es la brecha de seguridad más relevante del sprint. Se mitiga parcialmente con una vigencia corta (30 minutos) y se resolverá con lista de revocación apoyada en el `jti`.
- No hay MFA administrativo, que los lineamientos exigen. La decisión es diferirlo con justificación, no ignorarlo.
- El secreto simétrico HS256 es compartido por emisor y verificador: si se filtra, permite emitir tokens válidos. Con un único proceso el riesgo está acotado, pero migrar a RS256 sería obligatorio si otro servicio tuviera que verificar los tokens.
- El rol viaja dentro del token, de modo que un cambio de rol no surte efecto hasta que el token expira.
