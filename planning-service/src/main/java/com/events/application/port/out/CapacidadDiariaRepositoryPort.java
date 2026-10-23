package com.events.application.port.out;

import com.events.domain.entity.CapacidadDiaria;
import java.util.Optional;
import java.util.UUID;

public interface CapacidadDiariaRepositoryPort {
    CapacidadDiaria save(CapacidadDiaria capacidadDiaria);

    /**
     * Devuelve el limite diario vigente de un organizador (la fila mas reciente por fecha).
     */
    Optional<CapacidadDiaria> findCurrentByOrganizadorId(UUID organizadorId);
}
