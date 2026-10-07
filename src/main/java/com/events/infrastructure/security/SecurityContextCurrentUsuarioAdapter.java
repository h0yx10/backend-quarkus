package com.events.infrastructure.security;
import com.events.application.port.out.CurrentUsuarioPort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.UnauthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.UUID;
@RequestScoped
public class SecurityContextCurrentUsuarioAdapter implements CurrentUsuarioPort {
    @Inject SecurityIdentity identity;
    public UUID currentUsuarioId() {
        if(identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken jwt)) throw new UnauthorizedException();
        return UUID.fromString(jwt.getSubject());
    }
}
