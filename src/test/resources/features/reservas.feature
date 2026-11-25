# language: es
@aceptacion
Característica: Reservas sin sobrecupo y cancelación con anticipación (HU-07, HU-08)
  Como cliente
  Quiero reservar un turno disponible y poder cancelarlo con anticipación
  Para asegurar mi cita sin que el servicio venda más cupos de los que tiene

  # El reloj de pruebas marca el lunes 2026-10-05 a las 08:00 en America/Bogota.
  Antecedentes:
    Dado un proveedor con el servicio "Fisioterapia" de 60 minutos y capacidad 1
    Y el servicio atiende los lunes de "08:00" a "12:00"

  Escenario: Reservar un turno con cupo disponible
    Dado un cliente verificado con sesión iniciada
    Cuando el cliente reserva el turno del "2026-10-12" a las "09:00"
    Entonces la respuesta tiene el estado 201
    Y la reserva queda en estado "CONFIRMADA"
    Y el turno del "2026-10-12" a las "09:00" queda con 0 cupos disponibles

  Escenario: No se permite sobrecupo en un turno lleno
    Dado un cliente verificado con sesión iniciada
    Y el cliente ya reservó el turno del "2026-10-12" a las "09:00"
    Y otro cliente verificado con sesión iniciada
    Cuando el cliente reserva el turno del "2026-10-12" a las "09:00"
    Entonces la respuesta tiene el estado 409
    Y el código de error es "TURNO_SIN_CUPO"
    Y la respuesta sugiere turnos alternativos

  Escenario: El cliente cancela con al menos 5 días de anticipación
    Dado un cliente verificado con sesión iniciada
    Y el cliente ya reservó el turno del "2026-10-12" a las "09:00"
    Cuando el cliente cancela su reserva
    Entonces la respuesta tiene el estado 200
    Y la reserva queda en estado "CANCELADA"
    Y el turno del "2026-10-12" a las "09:00" queda con 1 cupos disponibles

  Escenario: El cliente no puede cancelar dentro de la ventana mínima de 5 días
    Dado un cliente verificado con sesión iniciada
    Y el cliente ya reservó el turno del "2026-10-12" a las "09:00"
    Y han pasado 3 días
    Cuando el cliente cancela su reserva
    Entonces la respuesta tiene el estado 422
    Y el código de error es "CANCELACION_FUERA_DE_PLAZO"

  Escenario: El proveedor cancela sin anticipación mínima (HU-15)
    Dado un cliente verificado con sesión iniciada
    Y el cliente ya reservó el turno del "2026-10-12" a las "09:00"
    Y han pasado 6 días
    Cuando el proveedor cancela la reserva
    Entonces la respuesta tiene el estado 200
    Y la reserva queda en estado "CANCELADA"
