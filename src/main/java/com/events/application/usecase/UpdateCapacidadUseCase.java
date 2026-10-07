package com.events.application.usecase;

import com.events.application.port.in.UpdateCapacidadPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.OrganizadorRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Organizador;
import com.events.domain.exception.OrganizadorNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateCapacidadUseCase implements UpdateCapacidadPort {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final OrganizadorRepositoryPort organizadorRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public UpdateCapacidadUseCase(CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                   OrganizadorRepositoryPort organizadorRepository,
                                   CurrentOrganizadorPort currentOrganizador) {
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.organizadorRepository = organizadorRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public CapacidadDiaria execute(BigDecimal limiteHoras) {
        var organizadorId = currentOrganizador.currentOrganizadorId();

        var existente = capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId);
        if (existente.isPresent()) {
            CapacidadDiaria capacidad = existente.get();
            capacidad.actualizarLimite(limiteHoras, LocalDate.now());
            return capacidadDiariaRepository.save(capacidad);
        }

        Organizador organizador = organizadorRepository.findById(organizadorId)
                .orElseThrow(() -> new OrganizadorNotFoundException("No encontramos el usuario autenticado."));
        return capacidadDiariaRepository.save(new CapacidadDiaria(organizador, LocalDate.now(), limiteHoras));
    }
}
