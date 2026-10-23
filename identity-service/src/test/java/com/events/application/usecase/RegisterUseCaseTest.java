package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.events.application.port.in.AuthResult;
import com.events.application.port.out.UsuarioRepositoryPort;
import com.events.application.port.out.PasswordHasherPort;
import com.events.application.port.out.RolRepositoryPort;
import com.events.application.port.out.TokenProviderPort;
import com.events.domain.entity.NombreRol;
import com.events.domain.entity.Rol;
import com.events.domain.exception.CorreoYaRegistradoException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegisterUseCaseTest {
    static class DirectTransaction implements com.events.application.port.out.TransactionPort {
        public <T> T execute(java.util.function.Supplier<T> action) { return action.get(); }
    }

    private final UsuarioRepositoryPort usuarioRepository = mock(UsuarioRepositoryPort.class);
    private final RolRepositoryPort rolRepository = mock(RolRepositoryPort.class);
    private final PasswordHasherPort passwordHasher = mock(PasswordHasherPort.class);
    private final TokenProviderPort tokenProvider = mock(TokenProviderPort.class);
    private final RegisterUseCase useCase =
            new RegisterUseCase(usuarioRepository, rolRepository, passwordHasher, tokenProvider, new DirectTransaction());

    @Test
    void registraConPasswordHasheadoRolOrganizadorYDevuelveToken() {
        when(usuarioRepository.existsByCorreo("camila@correo.com")).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ORGANIZADOR)).thenReturn(Optional.of(new Rol(NombreRol.ORGANIZADOR)));
        when(passwordHasher.hash("Secreta123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generate(any())).thenReturn("jwt");
        when(tokenProvider.expirationSeconds()).thenReturn(7200L);

        AuthResult result = useCase.execute(" Camila ", " Camila@Correo.com ", "Secreta123");

        assertThat(result.accessToken()).isEqualTo("jwt");
        assertThat(result.expiresInSeconds()).isEqualTo(7200L);
        assertThat(result.usuario().getCorreo()).isEqualTo("camila@correo.com");
        assertThat(result.usuario().getNombre()).isEqualTo("Camila");
        assertThat(result.usuario().getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(result.usuario().getRoles()).extracting(Rol::getNombre).containsExactly(NombreRol.ORGANIZADOR);
        // El perfil de organizador (solo usuario + activo) se crea junto con el usuario.
        assertThat(result.usuario().getOrganizador()).isNotNull();
        assertThat(result.usuario().getOrganizador().getUsuario()).isSameAs(result.usuario());
        assertThat(result.usuario().getOrganizador().isActivo()).isTrue();
    }

    @Test
    void exigeSemillaDeRoles() {
        when(rolRepository.findByNombre(NombreRol.ORGANIZADOR)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute("Camila", "camila@correo.com", "Secreta123"))
                .isInstanceOf(IllegalStateException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaCorreoDuplicado() {
        when(usuarioRepository.existsByCorreo("camila@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute("Camila", "CAMILA@correo.com", "Secreta123"))
                .isInstanceOf(CorreoYaRegistradoException.class);
        verify(usuarioRepository, never()).save(any());
    }
}
