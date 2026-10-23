package com.events.infrastructure.security;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

/**
 * Construye la identidad a partir de los headers que Traefik copia de la respuesta de
 * identity-service (forwardAuth). Este servicio no se publica: solo el gateway lo alcanza, y el
 * gateway borra estos headers si vienen del cliente.
 */
@ApplicationScoped
public class GatewayHeadersAuthenticationMechanism implements HttpAuthenticationMechanism {
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ORGANIZADOR_ID = "X-Organizador-Id";
    public static final String HEADER_ROLES = "X-Roles";
    public static final String ORGANIZADOR_ID = "organizadorId";

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        String userId = context.request().getHeader(HEADER_USER_ID);
        if (userId == null || userId.isBlank()) return Uni.createFrom().optional(java.util.Optional.empty());
        var builder = QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal(userId));
        String roles = context.request().getHeader(HEADER_ROLES);
        if (roles != null) Arrays.stream(roles.split(",")).map(String::trim).filter(r -> !r.isEmpty()).forEach(builder::addRole);
        String organizadorId = context.request().getHeader(HEADER_ORGANIZADOR_ID);
        if (organizadorId != null && !organizadorId.isBlank()) builder.addAttribute(ORGANIZADOR_ID, organizadorId);
        return Uni.createFrom().item(builder.build());
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer"));
    }

    /** Preserva el sobre JSON del API cuando se exige sesion. */
    @Override
    public Uni<Boolean> sendChallenge(RoutingContext context) {
        context.response().setStatusCode(401).putHeader("WWW-Authenticate", "Bearer").putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("success", false).put("message", RestAuthenticationErrorHandler.TOKEN_REQUIRED)
                        .put("timestamp", Instant.now().toString()).encode());
        return Uni.createFrom().item(true);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }
}
