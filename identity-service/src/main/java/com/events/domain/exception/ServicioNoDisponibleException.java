package com.events.domain.exception;

/** Otro servicio necesario para completar la operacion no respondio. */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
