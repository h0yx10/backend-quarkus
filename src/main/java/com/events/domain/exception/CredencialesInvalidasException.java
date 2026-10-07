package com.events.domain.exception;

/**
 * Correo inexistente, password incorrecto o cuenta inactiva. Se usa un unico mensaje para no
 * revelar cual de los tres casos ocurrio.
 */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
