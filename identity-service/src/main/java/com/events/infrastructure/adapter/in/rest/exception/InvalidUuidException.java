package com.events.infrastructure.adapter.in.rest.exception;
import jakarta.ws.rs.BadRequestException;
public class InvalidUuidException extends BadRequestException {
    public InvalidUuidException(String value,IllegalArgumentException cause) {
        super("Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'; "+cause.getMessage(),cause);
    }
}
