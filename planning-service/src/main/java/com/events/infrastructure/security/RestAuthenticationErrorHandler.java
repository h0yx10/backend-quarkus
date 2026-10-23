package com.events.infrastructure.security;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.vertx.ext.web.Router;
import java.time.Instant;
import io.vertx.core.json.JsonObject;
/** Errores de autenticacion HTTP, incluidos los que ocurren antes de invocar REST. */
@ApplicationScoped
public class RestAuthenticationErrorHandler {
    public static final String TOKEN_REQUIRED="Debes iniciar sesion para acceder a este recurso.";
    public static final String TOKEN_INVALID="Tu sesion expiro o el token no es valido. Inicia sesion nuevamente.";
    public static final String ACCESS_DENIED="No tienes permisos para acceder a este recurso.";
    void register(@Observes Router router) {
        router.route().order(Integer.MIN_VALUE).failureHandler(context->{
            int code=context.statusCode();
            Throwable error=context.failure();
            if(error instanceof io.quarkus.security.AuthenticationFailedException || error instanceof io.quarkus.security.UnauthorizedException) code=401;
            if(error instanceof io.quarkus.security.ForbiddenException) code=403;
            if(code!=401 && code!=403) { context.next(); return; }
            if(code==401) context.response().putHeader("WWW-Authenticate","Bearer");
            String message=code==403?ACCESS_DENIED:(context.request().getHeader("Authorization")==null?TOKEN_REQUIRED:TOKEN_INVALID);
            context.response().setStatusCode(code).putHeader("Content-Type","application/json")
                    .end(new JsonObject().put("success",false).put("message",message).put("timestamp",Instant.now().toString()).encode());
        });
    }
}
