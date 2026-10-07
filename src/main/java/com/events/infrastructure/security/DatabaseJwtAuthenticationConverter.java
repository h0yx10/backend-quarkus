package com.events.infrastructure.security;
import com.events.application.port.out.*;
import com.events.domain.entity.NombreRol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.enterprise.context.control.ActivateRequestContext;
import io.quarkus.security.identity.*;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.UUID;
/** REMOVED: Spring Converter. Reemplaza completamente los roles del token por los roles vigentes de BD. */
@ApplicationScoped
public class DatabaseJwtAuthenticationConverter implements SecurityIdentityAugmentor {
    @Inject UsuarioRepositoryPort usuarios;
    @Inject TokenRevocationPort revocations;
    public Uni<SecurityIdentity> augment(SecurityIdentity identity,AuthenticationRequestContext context) {
        if(identity.isAnonymous()) return Uni.createFrom().item(identity);
        return context.runBlocking(()->fromDatabase(identity));
    }
    @ActivateRequestContext
    SecurityIdentity fromDatabase(SecurityIdentity identity) {
        var credential=identity.getCredential(TokenCredential.class);
        if(credential==null || revocations.isRevoked(credential.getToken())) throw new AuthenticationFailedException();
        UUID id;
        try { id=UUID.fromString(((JsonWebToken)identity.getPrincipal()).getSubject()); }
        catch(RuntimeException ex) { throw new AuthenticationFailedException(ex); }
        var usuario=usuarios.findById(id).filter(u->u.puedeIniciarSesion()).orElseThrow(AuthenticationFailedException::new);
        var builder=QuarkusSecurityIdentity.builder().setPrincipal(identity.getPrincipal())
                .addCredentials(identity.getCredentials()).addAttributes(identity.getAttributes());
        usuario.getRoles().stream().filter(r->r.getNombre()!=NombreRol.ORGANIZADOR || usuario.getOrganizador()!=null)
                .forEach(r->builder.addRole(r.getNombre().name()));
        return builder.build();
    }
}
