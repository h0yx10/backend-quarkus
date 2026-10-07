package com.events.infrastructure.security;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.UnauthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class SecurityContextCurrentTokenAdapterTest {
    private SecurityContextCurrentTokenAdapter adapter(SecurityIdentity identity) {
        var result=new SecurityContextCurrentTokenAdapter();result.identity=identity;return result;
    }
    @Test
    void extraeTokenYExpiracionDelJwtValidado() {
        var identity=mock(SecurityIdentity.class);var jwt=mock(JsonWebToken.class);Instant expires=Instant.now().plusSeconds(600);
        when(identity.getPrincipal()).thenReturn(jwt);when(jwt.getClaim("exp")).thenReturn(expires.getEpochSecond());
        when(jwt.getExpirationTime()).thenReturn(expires.getEpochSecond());when(identity.getCredential(TokenCredential.class)).thenReturn(new TokenCredential("jwt","bearer"));
        assertThat(adapter(identity).tokenValue()).isEqualTo("jwt");assertThat(adapter(identity).expiresAt()).isEqualTo(Instant.ofEpochSecond(expires.getEpochSecond()));
    }
    @Test
    void rechazaContextoSinAutenticacion() { var identity=mock(SecurityIdentity.class);when(identity.isAnonymous()).thenReturn(true);assertThatThrownBy(adapter(identity)::tokenValue).isInstanceOf(UnauthorizedException.class); }
    @Test
    void rechazaPrincipalQueNoEsJwt() { var identity=mock(SecurityIdentity.class);when(identity.getPrincipal()).thenReturn(()->"usuario");assertThatThrownBy(adapter(identity)::expiresAt).isInstanceOf(UnauthorizedException.class); }
    @Test
    void rechazaJwtSinExpiracion() { var identity=mock(SecurityIdentity.class);when(identity.getPrincipal()).thenReturn(mock(JsonWebToken.class));assertThatThrownBy(adapter(identity)::tokenValue).isInstanceOf(UnauthorizedException.class); }
}
