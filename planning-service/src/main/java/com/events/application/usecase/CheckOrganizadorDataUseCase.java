package com.events.application.usecase;

import com.events.application.port.in.CheckOrganizadorDataPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.EventoRepositoryPort;
import java.util.UUID;

public class CheckOrganizadorDataUseCase implements CheckOrganizadorDataPort {

    private final EventoRepositoryPort eventoRepository;
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;

    public CheckOrganizadorDataUseCase(EventoRepositoryPort eventoRepository,
                                        CapacidadDiariaRepositoryPort capacidadDiariaRepository) {
        this.eventoRepository = eventoRepository;
        this.capacidadDiariaRepository = capacidadDiariaRepository;
    }

    @Override
    public boolean execute(UUID organizadorId) {
        return eventoRepository.existsByOrganizadorId(organizadorId)
                || capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId).isPresent();
    }
}
