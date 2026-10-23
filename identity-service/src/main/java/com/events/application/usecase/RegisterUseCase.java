package com.events.application.usecase;

import com.events.application.port.in.AuthResult;
import com.events.application.port.in.RegisterPort;
import com.events.application.port.out.UsuarioRepositoryPort;
import com.events.application.port.out.PasswordHasherPort;
import com.events.application.port.out.RolRepositoryPort;
import com.events.application.port.out.TokenProviderPort;
import com.events.domain.entity.NombreRol;
import com.events.domain.entity.Rol;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.CorreoYaRegistradoException;
import com.events.domain.entity.PasswordPolicy;
import com.events.application.port.out.TransactionPort;

public class RegisterUseCase implements RegisterPort {

    private final UsuarioRepositoryPort usuarioRepository;
    private final RolRepositoryPort rolRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenProviderPort tokenProvider;
    private final TransactionPort transaction;

    public RegisterUseCase(UsuarioRepositoryPort usuarioRepository, RolRepositoryPort rolRepository,
                           PasswordHasherPort passwordHasher, TokenProviderPort tokenProvider, TransactionPort transaction) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.transaction = transaction;
    }

    @Override
    public AuthResult execute(String nombre, String correo, String password) {
        PasswordPolicy.validar(password);
        Usuario guardado = transaction.execute(() -> {
            String correoNormalizado = Usuario.normalizarCorreo(correo);
            if (usuarioRepository.existsByCorreo(correoNormalizado)) {
                throw new CorreoYaRegistradoException("Ya existe una cuenta con ese correo.");
            }
            Rol rol = rolRepository.findByNombre(NombreRol.ORGANIZADOR)
                    .orElseThrow(() -> new IllegalStateException("Ejecuta docs/schema.sql: falta el rol ORGANIZADOR."));
            Usuario usuario = new Usuario(nombre.trim(), correoNormalizado, passwordHasher.hash(password));
            usuario.asignarRol(rol);
            usuario.habilitarComoOrganizador();
            return usuarioRepository.save(usuario);
        });

        return new AuthResult(tokenProvider.generate(guardado), tokenProvider.expirationSeconds(), guardado);
    }
}
