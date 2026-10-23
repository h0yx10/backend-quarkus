package com.events.application.usecase;

import com.events.application.port.in.AuthResult;
import com.events.application.port.in.LoginPort;
import com.events.application.port.out.UsuarioRepositoryPort;
import com.events.application.port.out.PasswordHasherPort;
import com.events.application.port.out.TokenProviderPort;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.CredencialesInvalidasException;
import java.util.Locale;

public class LoginUseCase implements LoginPort {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contrasena incorrectos.";

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenProviderPort tokenProvider;

    public LoginUseCase(UsuarioRepositoryPort usuarioRepository, PasswordHasherPort passwordHasher,
                        TokenProviderPort tokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResult execute(String correo, String password) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .filter(Usuario::puedeIniciarSesion)
                .filter(u -> passwordHasher.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new CredencialesInvalidasException(CREDENCIALES_INVALIDAS));

        return new AuthResult(tokenProvider.generate(usuario), tokenProvider.expirationSeconds(), usuario);
    }
}
