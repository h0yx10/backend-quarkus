package com.events.infrastructure.adapter.in.rest.exception;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.ws.rs.ext.*;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.Priority;
@Provider @Priority(1)
public class ValidationErrorMapper implements ExceptionMapper<ResteasyReactiveViolationException> {
    public Response toResponse(ResteasyReactiveViolationException ex) {
        return GlobalExceptionHandler.error(400,ex.getConstraintViolations().iterator().next().getMessage());
    }
}
