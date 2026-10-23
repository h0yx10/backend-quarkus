package com.events.application.usecase;

import com.events.application.port.in.GetCapacidadPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.domain.entity.CapacidadDiaria;

public class GetCapacidadUseCase implements GetCapacidadPort {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public GetCapacidadUseCase(CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                CurrentOrganizadorPort currentOrganizador) {
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public CapacidadDiaria execute() {
        return capacidadDiariaRepository.findCurrentByOrganizadorId(currentOrganizador.currentOrganizadorId())
                .orElse(null);
    }
}
