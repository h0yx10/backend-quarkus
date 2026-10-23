package com.events.infrastructure.security;
import io.quarkus.smallrye.jwt.runtime.auth.*;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import java.time.Instant;
/** Preserva el sobre JSON incluso cuando la autenticacion responde antes de Jakarta REST. */
@Singleton
public class ApiJwtAuthenticationMechanism extends JWTAuthMechanism {
    @Inject public ApiJwtAuthenticationMechanism(SmallRyeJwtConfig config) { super(config); }
    @Override public int getPriority() { return 2000; }
    @Override public Uni<Boolean> sendChallenge(RoutingContext context) {
        String message=context.request().getHeader("Authorization")==null?RestAuthenticationErrorHandler.TOKEN_REQUIRED:RestAuthenticationErrorHandler.TOKEN_INVALID;
        context.response().setStatusCode(401).putHeader("WWW-Authenticate","Bearer").putHeader("Content-Type","application/json")
                .end(new JsonObject().put("success",false).put("message",message).put("timestamp",Instant.now().toString()).encode());
        return Uni.createFrom().item(true);
    }
}
