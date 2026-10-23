package com.events.infrastructure.security;

import com.events.application.port.out.CurrentOrganizadorPort;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/** El organizador actual llega resuelto por identity-service; aqui no se consulta su base. */
@RequestScoped
public class GatewayCurrentOrganizadorAdapter implements CurrentOrganizadorPort {
    @Inject SecurityIdentity identity;

    @Override
    public UUID currentOrganizadorId() {
        if (identity.isAnonymous()) throw new UnauthorizedException();
        String organizadorId = identity.getAttribute(GatewayHeadersAuthenticationMechanism.ORGANIZADOR_ID);
        if (organizadorId == null) throw new ForbiddenException();
        try {
            return UUID.fromString(organizadorId);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException();
        }
    }
}
