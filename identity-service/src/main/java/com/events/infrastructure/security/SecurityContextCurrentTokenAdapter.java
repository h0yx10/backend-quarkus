package com.events.infrastructure.security;
import com.events.application.port.out.CurrentTokenPort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.UnauthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.time.Instant;
@RequestScoped
public class SecurityContextCurrentTokenAdapter implements CurrentTokenPort {
    @Inject SecurityIdentity identity;
    private JsonWebToken jwt() {
        if(identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken jwt) || jwt.getClaim("exp")==null) throw new UnauthorizedException();
        return jwt;
    }
    public String tokenValue() { jwt(); return identity.getCredential(TokenCredential.class).getToken(); }
    public Instant expiresAt() { return Instant.ofEpochSecond(jwt().getExpirationTime()); }
}
