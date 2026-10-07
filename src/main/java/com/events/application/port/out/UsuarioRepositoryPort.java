package com.events.application.port.out;

import com.events.domain.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {
    Usuario save(Usuario usuario);

    Optional<Usuario> findById(UUID id);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findAll();
    Optional<Usuario> findByIdForUpdate(UUID id);
    long countActiveAdmins();
    boolean hasBusinessData(UUID usuarioId);
    void delete(Usuario usuario);
}
