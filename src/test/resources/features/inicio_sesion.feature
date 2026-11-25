# language: es
@aceptacion
Característica: Inicio de sesión seguro y sesiones revocables (HU-03, ADR-0006)
  Como usuario de la plataforma
  Quiero que mi cuenta se bloquee ante intentos repetidos y que cerrar sesión invalide mis tokens
  Para que nadie pueda adivinar mi contraseña ni reutilizar una sesión robada

  Antecedentes:
    Dado un cliente registrado y verificado

  Escenario: Iniciar sesión con credenciales correctas
    Cuando inicia sesión con la contraseña correcta
    Entonces la respuesta tiene el estado 200
    Y recibe un token de acceso y un token de refresco

  Escenario: La cuenta se bloquea tras 5 intentos fallidos
    Dado que falló el inicio de sesión 5 veces
    Cuando inicia sesión con la contraseña correcta
    Entonces la respuesta tiene el estado 423
    Y el código de error es "CUENTA_BLOQUEADA"

  Escenario: El bloqueo vence después de 15 minutos
    Dado que falló el inicio de sesión 5 veces
    Y han pasado 16 minutos
    Cuando inicia sesión con la contraseña correcta
    Entonces la respuesta tiene el estado 200

  Escenario: Cerrar sesión revoca el token de refresco
    Dado que inició sesión con la contraseña correcta
    Cuando cierra la sesión
    Y intenta renovar la sesión con su token de refresco
    Entonces la respuesta tiene el estado 401
    Y el código de error es "CREDENCIALES_INVALIDAS"
