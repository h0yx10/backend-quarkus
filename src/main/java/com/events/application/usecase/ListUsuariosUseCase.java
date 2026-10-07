package com.events.application.usecase;

import com.events.application.port.in.ListUsuariosPort;
import com.events.application.port.out.UsuarioRepositoryPort;
import com.events.domain.entity.Usuario;
import java.util.List;

public class ListUsuariosUseCase implements ListUsuariosPort {

    private final UsuarioRepositoryPort usuarioRepository;

    public ListUsuariosUseCase(UsuarioRepositoryPort usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Usuario> execute() {
        return usuarioRepository.findAll();
    }
}
