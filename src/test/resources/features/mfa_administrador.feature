# language: es
@aceptacion
Característica: Acceso de administradores con segundo factor (MFA)
  Como administrador de la plataforma
  Quiero iniciar sesión con mi contraseña y un código enviado a mi correo
  Para que una contraseña filtrada no baste para entrar con privilegios de administrador

  Antecedentes:
    Dado un administrador aprovisionado desde la configuración

  Escenario: El administrador no puede entrar solo con su contraseña
    Cuando el administrador inicia sesión por el login normal
    Entonces la respuesta tiene el estado 403
    Y el código de error es "ACCESO_DENEGADO"

  Escenario: El administrador entra con contraseña y código
    Cuando el administrador solicita el código MFA con su contraseña
    Entonces la respuesta tiene el estado 202
    Y el administrador recibe un código de 6 dígitos por correo
    Cuando el administrador verifica el código recibido
    Entonces la respuesta tiene el estado 200
    Y la sesión tiene el rol "ADMIN"

  Escenario: Adivinar el código bloquea la cuenta
    Dado que el administrador solicitó el código MFA
    Y verificó 5 códigos incorrectos
    Cuando el administrador verifica el código recibido
    Entonces la respuesta tiene el estado 423
    Y el código de error es "CUENTA_BLOQUEADA"
