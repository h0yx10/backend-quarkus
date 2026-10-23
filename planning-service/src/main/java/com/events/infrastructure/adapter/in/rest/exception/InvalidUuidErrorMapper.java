package com.events.infrastructure.adapter.in.rest.exception;
import jakarta.ws.rs.ext.*;
import jakarta.ws.rs.core.Response;
@Provider
public class InvalidUuidErrorMapper implements ExceptionMapper<InvalidUuidException> {
    public Response toResponse(InvalidUuidException ex) { return GlobalExceptionHandler.error(400,ex.getMessage()); }
}
