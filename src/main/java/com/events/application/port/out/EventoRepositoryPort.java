package com.events.application.port.out;

import com.events.domain.entity.Evento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Las busquedas por id reciben siempre el organizador dueno: un evento de otro usuario se
 * comporta igual que uno inexistente (404), sin revelar que existe.
 */
public interface EventoRepositoryPort {
    Evento save(Evento evento);

    Optional<Evento> findByIdAndOrganizadorId(UUID id, UUID organizadorId);

    List<Evento> findByOrganizadorId(UUID organizadorId);

    void deleteById(UUID id);

    boolean existsByIdAndOrganizadorId(UUID id, UUID organizadorId);
}
