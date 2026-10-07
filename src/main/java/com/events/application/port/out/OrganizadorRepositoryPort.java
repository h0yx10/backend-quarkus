package com.events.application.port.out;

import com.events.domain.entity.Organizador;
import java.util.Optional;
import java.util.UUID;

public interface OrganizadorRepositoryPort {
    Optional<Organizador> findById(UUID id);
    Optional<Organizador> findByUsuarioId(UUID usuarioId);
}
