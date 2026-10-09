package com.events.infrastructure.adapter.in.rest.exception;
import com.events.domain.exception.*;
import com.events.infrastructure.utils.constants.MessageConstants;
import com.events.infrastructure.security.RestAuthenticationErrorHandler;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {
    private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Context HttpHeaders headers;
    public static Response error(int code,String message) {
        return Response.status(code).type(MediaType.APPLICATION_JSON)
                .entity(Map.of("success",false,"message",message,"timestamp",Instant.now().toString())).build();
    }
    @ServerExceptionMapper
    public Response malformed(MismatchedInputException ex) { return error(400,MessageConstants.INVALID_REQUEST); }
    @Override
    public Response toResponse(Throwable ex) {
        if(ex instanceof CapacityConflictException c) return Response.status(409).entity(Map.of("success",false,"code","overload_conflict","message",c.getMessage(),"timestamp",Instant.now().toString(),"plannedHours",c.getPlannedHours(),"limitHours",c.getLimitHours(),"exceedsBy",c.getExceedsBy())).build();
        if(ex instanceof CorreoYaRegistradoException || ex instanceof UsuarioConflictException) return error(409,ex.getMessage());
        if(ex instanceof CredencialesInvalidasException) return error(401,ex.getMessage());
        if(ex instanceof UsuarioNotFoundException || ex instanceof EventoNotFoundException || ex instanceof SubtareaNotFoundException || ex instanceof OrganizadorNotFoundException) return error(404,ex.getMessage());
        if(ex instanceof ConstraintViolationException c) return error(400,c.getConstraintViolations().iterator().next().getMessage());
        if(ex instanceof IllegalArgumentException) return error(400,ex.getMessage());
        if(ex instanceof JsonProcessingException || ex instanceof BadRequestException) return error(400,MessageConstants.INVALID_REQUEST);
        if(ex instanceof io.quarkus.security.AuthenticationFailedException || ex instanceof io.quarkus.security.UnauthorizedException) return error(401,headers.getHeaderString("Authorization")==null?RestAuthenticationErrorHandler.TOKEN_REQUIRED:RestAuthenticationErrorHandler.TOKEN_INVALID);
        if(ex instanceof io.quarkus.security.ForbiddenException) return error(403,RestAuthenticationErrorHandler.ACCESS_DENIED);
        if(ex instanceof WebApplicationException w) return error(w.getResponse().getStatus(),MessageConstants.INVALID_REQUEST);
        log.error("Error inesperado al procesar la solicitud",ex); return error(500,MessageConstants.UNEXPECTED_ERROR);
    }
}
