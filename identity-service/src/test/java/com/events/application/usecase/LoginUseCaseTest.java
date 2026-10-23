package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.UsuarioRepositoryPort;
import com.events.application.port.out.PasswordHasherPort;
import com.events.application.port.out.TokenProviderPort;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.CredencialesInvalidasException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LoginUseCaseTest {

    private final UsuarioRepositoryPort usuarioRepository = mock(UsuarioRepositoryPort.class);
    private final PasswordHasherPort passwordHasher = mock(PasswordHasherPort.class);
    private final TokenProviderPort tokenProvider = mock(TokenProviderPort.class);
    private final LoginUseCase useCase = new LoginUseCase(usuarioRepository, passwordHasher, tokenProvider);

    @Test
    void devuelveTokenConCredencialesCorrectas() {
        Usuario organizador = new Usuario("Camila", "camila@correo.com", "$2a$hash");
        when(usuarioRepository.findByCorreo("camila@correo.com")).thenReturn(Optional.of(organizador));
        when(passwordHasher.matches("Secreta123", "$2a$hash")).thenReturn(true);
        when(tokenProvider.generate(organizador)).thenReturn("jwt");

        var result = useCase.execute("Camila@Correo.com", "Secreta123");

        assertThat(result.accessToken()).isEqualTo("jwt");
        assertThat(result.usuario()).isSameAs(organizador);
    }

    @Test
    void fallaConPasswordIncorrecto() {
        Usuario organizador = new Usuario("Camila", "camila@correo.com", "$2a$hash");
        when(usuarioRepository.findByCorreo("camila@correo.com")).thenReturn(Optional.of(organizador));
        when(passwordHasher.matches("otra", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("camila@correo.com", "otra"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void fallaConCorreoInexistente() {
        when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("nadie@correo.com", "Secreta123"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void fallaSiElOrganizadorEstaInactivo() {
        Usuario usuario = new Usuario("Camila", "camila@correo.com", "$2a$hash");
        usuario.habilitarComoOrganizador();
        usuario.getOrganizador().desactivar();
        when(usuarioRepository.findByCorreo("camila@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("Secreta123", "$2a$hash")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute("camila@correo.com", "Secreta123"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void fallaSiLaCuentaNoTienePassword() {
        // Ej. el antiguo organizador demo, creado antes de existir el login.
        Usuario sinPassword = new Usuario("Demo", "demo@organizador.local", null);
        when(usuarioRepository.findByCorreo("demo@organizador.local")).thenReturn(Optional.of(sinPassword));

        assertThatThrownBy(() -> useCase.execute("demo@organizador.local", "cualquiera"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
