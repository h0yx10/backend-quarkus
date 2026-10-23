package com.events.infrastructure.security;
import io.smallrye.jwt.auth.principal.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
/** Preserva exactamente la clave UTF-8 usada por Spring, sin reinterpretarla como JWK. */
@ApplicationScoped @Alternative @Priority(1)
public class Hs256PrincipalFactory extends JWTCallerPrincipalFactory {
    private final SecretKeySpec key;
    @Inject
    public Hs256PrincipalFactory(@ConfigProperty(name="app.security.jwt.secret") String secret) {
        byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);
        if(bytes.length<32) throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes para HS256.");
        key=new SecretKeySpec(bytes,"HmacSHA256");
    }
    public JWTCallerPrincipal parse(String token,JWTAuthContextInfo configured) throws ParseException {
        var context=new JWTAuthContextInfo(configured);
        context.setSecretKeyContent(null);context.setSecretVerificationKey(key);
        return new DefaultJWTCallerPrincipalFactory().parse(token,context);
    }
}
