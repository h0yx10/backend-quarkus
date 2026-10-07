package com.events.application.usecase;

import com.events.application.port.in.GetCurrentUserPort;
import com.events.application.port.out.CurrentUsuarioPort;
import com.events.application.port.out.UsuarioRepositoryPort;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.CredencialesInvalidasException;

public class GetCurrentUserUseCase implements GetCurrentUserPort {

    private final UsuarioRepositoryPort usuarioRepository;
    private final CurrentUsuarioPort currentUsuario;

    public GetCurrentUserUseCase(UsuarioRepositoryPort usuarioRepository,
                                 CurrentUsuarioPort currentUsuario) {
        this.usuarioRepository = usuarioRepository;
        this.currentUsuario = currentUsuario;
    }

    @Override
    public Usuario execute() {
        return usuarioRepository.findById(currentUsuario.currentUsuarioId())
                .orElseThrow(() -> new CredencialesInvalidasException("No encontramos el usuario autenticado."));
    }
}
