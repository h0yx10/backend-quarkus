package com.events.infrastructure.security;
import com.events.application.port.out.TokenProviderPort;
import com.events.domain.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
@ApplicationScoped
public class JwtTokenProviderAdapter implements TokenProviderPort {
    @ConfigProperty(name="app.security.jwt.secret") String secret;
    @ConfigProperty(name="app.security.jwt.issuer") String issuer;
    @ConfigProperty(name="app.security.jwt.expiration-minutes") long minutes;
    public String generate(Usuario usuario) {
        byte[] key=secret.getBytes(StandardCharsets.UTF_8);
        if(key.length<32) throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes para HS256.");
        long now=Instant.now().getEpochSecond();
        Map<String,Object> claims=new LinkedHashMap<>();
        claims.put("sub",usuario.getId().toString()); claims.put("iss",issuer); claims.put("iat",now);
        claims.put("exp",now+expirationSeconds()); claims.put("jti",UUID.randomUUID().toString());
        claims.put("nombre",usuario.getNombre()); claims.put("correo",usuario.getCorreo());
        claims.put("roles",usuario.getRoles().stream().map(r->r.getNombre().name()).sorted().toList());
        return Jwt.claims(claims).jws().algorithm(SignatureAlgorithm.HS256).sign(new SecretKeySpec(key,"HmacSHA256"));
    }
    public long expirationSeconds() { return minutes*60; }
}
